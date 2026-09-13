"""Monotonic release identity from this workflow's GitHub run number.
Reruns retain an identity; a new workflow run always gets a higher code.
"""
import os
import re

run = os.environ.get('GITHUB_RUN_NUMBER', '')
if not re.fullmatch(r'[1-9][0-9]{0,6}', run):
    raise SystemExit('A positive GitHub run number is required for publication.')
code = 100_000 + int(run)
version = f'1.2.{int(run)}'
with open(os.environ['GITHUB_ENV'], 'a') as output:
    output.write(f'LIFEMATE_VERSION_CODE={code}\nLIFEMATE_VERSION_NAME={version}\n')
print(f'Release version: {version} ({code})')
