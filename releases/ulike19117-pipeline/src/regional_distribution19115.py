"""Strict shipped-artifact scope, distinct from positive forced research fixtures."""
EXPECTED = dict(status='passed', experimental_helper_and_nested_absent=True,
                direct_experimental_references_absent=True,
                standard_selection_apis_preserved=True, optional_reflection_sites=5,
                negative_controls=7, distributed_regional_auto_adoption=False,
                physical_android_tested=False)

def verify(value):
    if type(value) is not dict or set(value) != set(EXPECTED):
        raise AssertionError('Distributed regional-helper DEX proof absent or extra fields')
    for key, expected in EXPECTED.items():
        if type(value[key]) is not type(expected) or value[key] != expected:
            raise AssertionError('Distributed regional-helper DEX proof differs: '+key)
    return value
