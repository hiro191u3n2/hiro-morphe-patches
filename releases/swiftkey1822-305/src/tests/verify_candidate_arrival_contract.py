#!/usr/bin/env python3
"""Verify pinned native arrival/lifecycle facts used by the host regressions."""
import hashlib
import json
import re
from pathlib import Path

FIXTURE_SHA256 = 'e8c02f103ee055567dc2c2425bec9e73a5106eba28cfdad4ab8a0b7e05ce913d'


def verify(root, native=None):
    path = Path(root) / 'tests/native_candidate_arrival_fixture.json'
    raw = path.read_bytes()
    assert hashlib.sha256(raw).hexdigest() == FIXTURE_SHA256, 'Native arrival fixture drift'
    fixture = json.loads(raw)
    fragments = fixture['fragments']
    for entry in fragments.values():
        assert hashlib.sha256(entry['smali'].encode()).hexdigest() == entry['fragment_sha256']
        if native:
            text = (Path(native) / entry['path']).read_text()
            method = re.search(r'^\.method[^\n]* ' + re.escape(entry['signature'])
                               + r'\n.*?^\.end method$', text, re.M | re.S).group(0)
            assert hashlib.sha256(method.encode()).hexdigest() == entry['full_method_sha256']
    def body(name):
        return fragments[name]['smali']
    def ordered(name, *parts):
        text, start = body(name), 0
        for part in parts:
            found = text.find(part, start)
            assert found >= 0, (name, part)
            start = found + len(part)
    ordered('subscription', 'Ljava/util/EnumSet;->allOf', 'Lwb0/t0;->Z',
            'Llb0/a;->e:Lib0/a;', 'Lib0/s;->a(Lib0/a;)V')
    ordered('main_queue', 'Ln50/a;->a:Landroid/os/Handler;',
            'Landroid/os/Handler;->post(Ljava/lang/Runnable;)Z')
    ordered('result_delivery', 'Ldt/p;->a:Ljava/lang/Object;', 'check-cast v5, Lib0/m;',
            'Lib0/a;-><init>', 'Lwb0/k;->get()', 'Lib0/s;->a(Lib0/a;)V')
    # This exact main-queue callback has no current-task/cancellation/epoch
    # check between its captured event type and delivery to registered views.
    assert 'cancel' not in body('result_delivery') and 'Lib0/p;->h' not in body('result_delivery')
    ordered('provider', 'Lib0/a;->b:Lib0/m;', 'Lib0/m;->g:Lib0/m;',
            'const/4 p1, 0x1', 'Lrb0/j;->b:Ljava/util/List;', 'Lfb0/u;->h(Z)V')
    ordered('base_attach', 'Lxd0/d1;->onAttachedToWindow()', 'Lrb0/d;->j()V')
    ordered('panel_attach', 'Lrb0/d;->onAttachedToWindow()', 'Lfb0/u;->i(Lfb0/s;)V',
            'iput-boolean v3, v1, Lrb0/h;->r:Z', 'Lrb0/h;->G()V',
            'Lra/q1;->n()V', 'Landroidx/recyclerview/widget/RecyclerView;->w0(I)V')
    ordered('panel_detach', 'const/4 v0, 0x0', 'iput-boolean v0, v1, Lrb0/h;->r:Z',
            'Lp10/a;->remove(Ljava/lang/Object;)Z')
    ordered('task_submission', 'Lib0/p;->i:Z', 'if-nez v2, :cond_0',
            'Lir/i;->cancel(Z)Z', 'Lib0/p;->i:Z', 'Lcom/touchtype_fluency/service/s;->b')
    for name in ('DEFAULT', 'TAP', 'EXPANDED', 'EDITOR_STATE_UPDATED'):
        assert '"' + name + '"' in body('event_types')
    return {
        'status': 'PASSED', 'fixture_sha256': FIXTURE_SHA256,
        'native_methods_or_fragments': len(fragments),
        'all_result_types_reach_expanded_provider': True,
        'posted_result_uses_captured_type_without_current_task_check': True,
        'non_expanded_type_uses_native_reset_path': True,
        'reattach_cached_result_precedes_active_adapter_and_scroll_to_zero': True,
        'source_compared_to_decoded_native': bool(native),
        'native_method_sha256': {key: entry['full_method_sha256'] for key, entry in fragments.items()},
        'scope': 'Static native dataflow and lifecycle verification; host scenarios replay these contracts, not native Android rendering',
        'device_tested': False,
    }


if __name__ == '__main__':
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument('--source-dir', type=Path, required=True)
    parser.add_argument('--native-dir', type=Path)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    result = verify(args.source_dir, args.native_dir)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
    print('PASS native candidate arrival/lifecycle contract: 9 pinned methods/fragments')
