"""Synthetic tensors only. No private graph or model bytes are embedded in this test."""
import unittest
import numpy as np
import onnx
import onnxruntime as ort
from onnx import helper, TensorProto
from onnx.reference import ReferenceEvaluator
from operator_reference import bilinear_half_pixel_2x_nchw, resize_onnx_node


class ResizeContract(unittest.TestCase):
    def check_tensor(self, x):
        expected = bilinear_half_pixel_2x_nchw(x)
        node, sizes = resize_onnx_node("upsample", "X", "Y", x.shape)
        graph = helper.make_graph([node], "synthetic-resize", [helper.make_tensor_value_info(
            "X", TensorProto.FLOAT, list(x.shape))], [helper.make_tensor_value_info(
            "Y", TensorProto.FLOAT, list(expected.shape))], [sizes])
        model = helper.make_model(graph, opset_imports=[helper.make_opsetid("", 18)], ir_version=10)
        onnx.checker.check_model(model, full_check=True)
        ref = ReferenceEvaluator(model).run(None, {"X": x})[0]
        options = ort.SessionOptions()
        options.intra_op_num_threads = 1
        options.inter_op_num_threads = 1
        options.graph_optimization_level = ort.GraphOptimizationLevel.ORT_DISABLE_ALL
        runtime = ort.InferenceSession(model.SerializeToString(), options, providers=["CPUExecutionProvider"])
        actual = runtime.run(None, {"X": x})[0]
        np.testing.assert_allclose(ref, expected, rtol=1e-6, atol=1e-6)
        np.testing.assert_allclose(actual, expected, rtol=1e-6, atol=1e-6)
        # Explicitly test the two clamped edges; no align-corners or asymmetric substitution.
        np.testing.assert_array_equal(actual[:, :, 0, 0], x[:, :, 0, 0])
        np.testing.assert_array_equal(actual[:, :, -1, -1], x[:, :, -1, -1])

    def test_ramp_and_channel_isolation(self):
        self.check_tensor(np.arange(2*3*4*5, dtype=np.float32).reshape(2,3,4,5))

    def test_single_pixel_axis(self):
        self.check_tensor(np.array([[[[3.0], [-4.0], [9.0]]]], dtype=np.float32))
        self.check_tensor(np.array([[[[3.0, -4.0, 9.0]]]], dtype=np.float32))

    def test_random_tensor(self):
        self.check_tensor(np.random.default_rng(168).uniform(-2,2,(1,4,7,9)).astype(np.float32))

    def test_rejects_non_fp32_or_nonfinite(self):
        with self.assertRaises(ValueError): bilinear_half_pixel_2x_nchw(np.ones((1,1,2,2),np.float64))
        with self.assertRaises(ValueError): bilinear_half_pixel_2x_nchw(np.full((1,1,2,2),np.nan,np.float32))


if __name__ == "__main__":
    unittest.main()
