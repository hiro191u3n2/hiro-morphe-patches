import tempfile
import unittest
import warnings
from pathlib import Path
from zipfile import ZipFile

from style_mask import load_neural_mask, PINS


class MaskGuards(unittest.TestCase):
    def test_style_and_orientation_required(self):
        with self.assertRaises(ValueError):load_neural_mask("missing.zip","other",vertical_flip=False)
        with self.assertRaises(ValueError):load_neural_mask("missing.zip","natural_blush",vertical_flip=None)

    def test_missing_duplicate_and_wrong_hash(self):
        member,size,_,_=PINS["natural_blush"]
        with tempfile.TemporaryDirectory() as d:
            p=Path(d)/"mask.zip"
            with ZipFile(p,"w") as z:z.writestr("irrelevant.txt","x")
            with self.assertRaises(ValueError):load_neural_mask(p,"natural_blush",vertical_flip=False)
            with warnings.catch_warnings():
                warnings.simplefilter("ignore",UserWarning)
                with ZipFile(p,"w") as z:
                    z.writestr(member,b"0"*size)
                    z.writestr(member,b"0"*size)
            with self.assertRaises(ValueError):load_neural_mask(p,"natural_blush",vertical_flip=False)
            with ZipFile(p,"w") as z:z.writestr(member,b"0"*size)
            with self.assertRaises(ValueError):load_neural_mask(p,"natural_blush",vertical_flip=False)


if __name__=="__main__":unittest.main()
