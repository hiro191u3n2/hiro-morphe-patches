# Unpublished preview-repair precursor

These Preview177/Transform177 sources and LOCAL_QA.json describe a **never-published black-preview candidate**, not the publicly released ULike v1.7.7 chroma-mitigation patch. The publication preflight detected that the chroma release had advanced the live version, and correctly refused to overwrite it.

The preview repair is rebased onto that actual published chroma binary and delivered as **ULike v1.7.8 / Hiro Morphe v1.0.111**. Its release inputs and definitive QA are in `../ulike-v1.7.8/`; `rebase178.py` retains these reviewed precursor source files as build inputs. No preview v1.7.7 MPP was published. Both releases remain device-untested; local APK rebuild tests are not Galaxy camera execution.
