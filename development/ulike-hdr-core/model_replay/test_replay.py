"""Synthetic test graphs and weights only; no extracted model data is embedded."""
import unittest
import numpy as np

from baoman_replay import _plan,build_onnx,cpu_session,run_tensor
from graph_schema import parse_graph,UnsupportedGraph
from reference_nhwc import reference_layers


def weights(values):
    return np.asarray(values,dtype="<f4").tobytes()


class ReplayContracts(unittest.TestCase):
    def test_conv_ohwi_channel_order_bias(self):
        text="""1 1 9
DataV2 input 1 2 2 2 4 0 0
Convolution conv 3 1 1 1 1 0 0 1 0 4 0 4 0 4 0 input output
"""
        plan=_plan(parse_graph(text),weights([1,2,3,4,5,6,10,20,30]))
        x=np.array([[[[1,2],[3,4]],[[10,20],[30,40]]]],np.float32)
        y=run_tensor(plan,x)
        expected=np.stack([x[:,0]+2*x[:,1]+10,3*x[:,0]+4*x[:,1]+20,5*x[:,0]+6*x[:,1]+30],axis=1)
        np.testing.assert_array_equal(y,expected)

    def test_dw_hwc_spatial_order_relu(self):
        text="""1 1 9
DataV2 input 1 3 3 2 4 0 0
DepthwiseSeparableConvolution dw 2 3 3 1 1 0 0 1 1 4 0 4 0 4 0 input output
"""
        raw=np.arange(18,dtype=np.float32).reshape(3,3,2)-7
        plan=_plan(parse_graph(text),raw.tobytes()+weights([-1000,10]))
        x=np.arange(18,dtype=np.float32).reshape(1,2,3,3)
        y=run_tensor(plan,x)
        expected=np.maximum(np.array([sum(x[0,c,j,k]*raw[j,k,c] for j in range(3) for k in range(3)) for c in range(2)])+[-1000,10],0)
        np.testing.assert_array_equal(y.reshape(2),expected)

    def test_all_operators_spatial_channels_and_residual(self):
        text="""1 7 9
DataV2 input 1 6 6 2 4 0 0
Convolution conv 2 3 3 2 2 1 1 1 1 4 0 4 0 4 0 input a
DepthwiseSeparableConvolution dw 2 3 3 1 1 1 1 1 0 4 0 4 0 4 0 a b
Eltwise sum a b c 4 0 1
Concat concat 2 a c d 4 0
Convolution point 3 1 1 1 1 0 0 1 0 4 0 4 0 4 0 d e
UpSampling resize e f BILINEAR
Tanh tanh f output 4 0
"""
        rng=np.random.default_rng(5168)
        count=2*3*3*2+2+3*3*2+2+3*4+3
        plan=_plan(parse_graph(text),weights(rng.uniform(-.2,.2,count)))
        x=rng.uniform(-1,1,plan.input_shape).astype(np.float32)
        ys=cpu_session(build_onnx(plan,True)).run(None,{"input":x})
        for actual,(_,reference) in zip(ys,reference_layers(plan,x)):
            np.testing.assert_allclose(actual,reference,rtol=2e-5,atol=2e-5)
        self.assertEqual(ys[-1].shape,(1,3,6,6))

    def test_reject_weight_truncation_extra_nan_and_unsupported_geometry(self):
        text="""1 1 9
DataV2 input 1 3 3 2 4 0 0
Convolution conv 3 1 1 1 1 0 0 1 0 4 0 4 0 4 0 input output
"""
        graph=parse_graph(text)
        for raw in [b"x",weights([0]*8),weights([0]*10),weights([np.nan]*9)]:
            with self.subTest(size=len(raw)):
                with self.assertRaises(UnsupportedGraph): _plan(graph,raw)
        bad=text.replace("3 1 1 1 1 0 0 1 0","3 1 3 1 1 0 0 1 0")
        with self.assertRaises(UnsupportedGraph): _plan(parse_graph(bad),weights([0]*21))

    def test_reject_input_shape_dtype_and_nonfinite(self):
        text="""1 1 9
DataV2 input 1 2 2 1 4 0 0
Tanh tanh input output 4 0
"""
        plan=_plan(parse_graph(text),b"")
        for x in [np.zeros((1,1,2,2),np.float64),np.zeros((1,1,4,4),np.float32),np.full((1,1,2,2),np.inf,np.float32)]:
            with self.assertRaises(ValueError): run_tensor(plan,x)


if __name__=="__main__": unittest.main()
