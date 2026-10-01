import unittest
from graph_schema import parse_graph, UnsupportedGraph

# Synthetic graph declarations only, not an extracted vendor model.
GOOD = """1 3 123
DataV2 input 1 4 4 3 4 0 0
Convolution conv 4 3 3 1 1 1 1 1 1 4 0 4 0 4 0 input c
UpSampling resize c r BILINEAR
Tanh activation r out 4 0
"""


class SchemaTests(unittest.TestCase):
    def test_preserves_structure(self):
        g = parse_graph(GOOD)
        self.assertEqual(g.input_nhwc, (1,4,4,3))
        self.assertEqual(g.nodes[0].parameters[:9], (4,3,3,1,1,1,1,1,1))
        self.assertEqual(g.output_name, "out")

    def test_strict_rejections(self):
        for text in [GOOD.replace("1 3 123", "1 2 123"), GOOD+"\0", GOOD.replace("BILINEAR", "NEAREST"),
                     GOOD.replace("Convolution", "OpaqueCustomKernel"), GOOD.replace("input c", "missing c"),
                     GOOD.replace("4 0 4 0 4 0", "2 0 4 0 4 0"), GOOD.replace("r out", "r input"),
                     GOOD.replace("1 4 4 3", "1 9999999999 4 3"), GOOD.replace("1 3 123", "1 3 4294967296")]:
            with self.subTest(text=text[:30]):
                with self.assertRaises(UnsupportedGraph): parse_graph(text)


if __name__ == "__main__": unittest.main()
