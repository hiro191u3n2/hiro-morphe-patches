"""Executed-output requirements for the two reviewed .115 CPU changes."""
KEYS={
 'front_lag115':('actual_arm_neon_executed','existing_jni_transaction_fixture','test_counters_only'),
 'neon_gather115':('actual_arm64_execution','independent_frozen_scalar_oracle','production_arithmetic_unchanged'),
}
FRONT_CASES={'pixels':('pixel_cases',320),'safeguards':('safeguard_cases',32768),'pairs':('pair_cases',512),'coefficients':('coefficient_cases',8192)}
NEON_COUNTS={'assertions':5172261,'pixel_comparisons':194304,'guide_float_bit_comparisons':497728,'gather_raw_word_comparisons':4615616,'interior_cases':143129,'edge_cases':1109,'uncached_cases':144238,'mutants_detected':2}
def positive(tests,need):
 front=tests['front_lag115']
 need(front.get('schema')=='ulike19115-front-lag-proof-v1' and front.get('save_2000ms_achieved') is False,'Front .115 evidence identity/device scope differs')
 need(type(front.get('assertions')) is int and front['assertions']>=632253,'Front .115 coverage missing')
 contract=front.get('source_contract')
 need(type(contract) is dict and contract.get('status')=='passed' and contract.get('exactly_one_control_flow_change') is True and contract.get('computed_reduction_order_unchanged') is True and contract.get('floating_environment_flags_claimed_identical') is False and type(contract.get('negative_controls')) is int and contract['negative_controls']>=3 and contract.get('baseline_sha256')=='a7310416487c63bb2c08c3a45e4b28f3fbfb11c965b1094e1e2a2fa498a04b8b','Front .115 exact source/control-flow proof missing')
 architectures=front.get('architectures')
 need(type(architectures) is dict and set(architectures)=={'host','arm64'},'Both front .115 architectures required')
 for arch,row in architectures.items():
  need(row.get('raw_bits_exact') is True,'Front .115 raw output mismatch: '+arch)
  for section,(key,count) in FRONT_CASES.items():
   value=row.get('cases',{}).get(section,{}).get(key)
   need(type(value) is int and value==count,'Front .115 case coverage differs: '+arch+'/'+section)
  for key,count in [('required_lag_negative_controls',2),('baseline_lag_calls',61440),('candidate_lag_calls',33769),('lag_calls_removed',27671)]:
   need(type(row.get(key)) is int and row[key]==count,'Front .115 executed lag work accounting differs: '+arch+'/'+key)
  outputs=row.get('branch_outputs')
  need(type(outputs) is dict and set(outputs)=={'baseline','candidate','remove-needed-lag1','remove-needed-lag4'} and isinstance(outputs['baseline'],str) and bool(outputs['baseline']) and outputs['baseline']==outputs['candidate'] and all(isinstance(outputs[k],str) and bool(outputs[k]) and outputs[k]!=outputs['baseline'] for k in ('remove-needed-lag1','remove-needed-lag4')),'Required lag branch mutation was not detected: '+arch)
 neon=tests['neon_gather115']
 for key,count in NEON_COUNTS.items():need(type(neon.get(key)) is int and neon[key]>=count,'NEON .115 executed coverage differs: '+key)
 need(neon.get('save_2000ms_verified') is False and neon.get('baseline_header_sha256')=='0f5f589a2d641e0929541a0c509af0212ddb8ec15e2a8b87b2f11b6d9d917b04','NEON .115 independent reference/device scope differs')
 code=neon.get('android_assembly')
 need(type(code) is dict and set(code)=={'baseline','candidate'},'Both Android gather assembly records required')
 for label,count,loads in [('baseline',77,0),('candidate',33,1)]:
  row=code[label]
  need(type(row.get('cached_hit_path_instructions')) is int and row['cached_hit_path_instructions']==count and type(row.get('ld3_count')) is int and row['ld3_count']==loads,'Reviewed Android gather code generation differs: '+label)

def parser_fixtures():
 rows={name:dict(status='passed',assertions=2,physical_android_tested=False,device_speedup_verified=False,**{key:True for key in keys}) for name,keys in KEYS.items()}
 rows['front_lag115'].update(schema='ulike19115-front-lag-proof-v1',assertions=632253,save_2000ms_achieved=False,source_contract=dict(status='passed',negative_controls=3,baseline_sha256='a7310416487c63bb2c08c3a45e4b28f3fbfb11c965b1094e1e2a2fa498a04b8b',exactly_one_control_flow_change=True,computed_reduction_order_unchanged=True,floating_environment_flags_claimed_identical=False),architectures={arch:dict(raw_bits_exact=True,cases={section:{key:count} for section,(key,count) in FRONT_CASES.items()},required_lag_negative_controls=2,baseline_lag_calls=61440,candidate_lag_calls=33769,lag_calls_removed=27671,branch_outputs={'baseline':'parser-good','candidate':'parser-good','remove-needed-lag1':'parser-bad1','remove-needed-lag4':'parser-bad4'}) for arch in ('host','arm64')})
 rows['neon_gather115'].update(**NEON_COUNTS,save_2000ms_verified=False,baseline_header_sha256='0f5f589a2d641e0929541a0c509af0212ddb8ec15e2a8b87b2f11b6d9d917b04',android_assembly={label:dict(cached_hit_path_instructions=count,ld3_count=loads) for label,count,loads in [('baseline',77,0),('candidate',33,1)]})
 return rows
