"""Run three clean Paper/Purpur phases with only Casino and a vanilla-display probe."""
import argparse
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import shutil
import socket
import subprocess
import zipfile
import xml.etree.ElementTree as ET

HERE = Path(__file__).resolve().parent
CASINO = HERE.parents[1]
MARKERS = ('CASINO_VANILLA_FIRST_PASS', 'CASINO_VANILLA_RESTART_PASS',
           'CASINO_VANILLA_DELETE_RESTART_PASS')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--port', type=int, default=25597)
    parser.add_argument('--server-template', type=Path, required=True,
                        help='Prepared server folder with accepted EULA, libraries, cache and versions')
    parser.add_argument('--server-version', choices=('1.21.8', '1.21.9', '1.21.10', '1.21.11',
                                                   '26.1.2', '26.2', '26.3'), default='26.2')
    parser.add_argument('--server-jar', default='purpur-2622.jar',
                        help='Bootstrap JAR filename in the template folder')
    parser.add_argument('--java-home', type=Path, default=os.environ.get('JAVA_HOME'))
    args = parser.parse_args()
    if not args.java_home:
        parser.error('Set JAVA_HOME or pass --java-home (JDK 25).')
    base = args.server_template.resolve()
    jdk = args.java_home / 'bin'
    suffix = '.exe' if os.name == 'nt' else ''
    if 'eula=true' not in (base / 'eula.txt').read_text(encoding='utf-8'):
        parser.error('Accept the EULA in your server template before running this probe.')
    with socket.socket() as check:
        check.bind(('127.0.0.1', args.port))
    version = ET.parse(CASINO / 'pom.xml').findtext('{http://maven.apache.org/POM/4.0.0}version')
    plugin = CASINO / f'target/3dcasino-{version}.jar'
    probe_build = CASINO / 'target' / ('probe-' + datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ'))
    classes = probe_build / 'classes'
    classes.mkdir(parents=True, exist_ok=True)
    server_jars = sorted((base / 'versions' / args.server_version).glob('*.jar'))
    if len(server_jars) != 1:
        parser.error(f'Expected one patched server JAR for {args.server_version}, found {server_jars}')
    jars = [plugin, *server_jars,
            *sorted((base / 'libraries').rglob('*.jar'))]
    subprocess.run([str(jdk / ('javac' + suffix)), '-encoding', 'UTF-8', '-cp',
                    os.pathsep.join(map(str, jars)), '-d', str(classes),
                    str(HERE / 'CasinoVanillaProbe.java')], check=True)
    probe = probe_build / 'CasinoVanillaProbe.jar'
    with zipfile.ZipFile(probe, 'w', zipfile.ZIP_DEFLATED) as archive:
        archive.writestr('plugin.yml', 'name: CasinoVanillaProbe\nversion: 1\n'
                         "api-version: '1.21.8'\nmain: dev.casino3d.probe.CasinoVanillaProbe\n"
                         'depend: [3dcasino]\n')
        for file in sorted(classes.rglob('*.class')):
            archive.write(file, file.relative_to(classes).as_posix())
    run = CASINO / 'reports/vanilla-runtime' / ('run-' + datetime.now(timezone.utc)
                                                .strftime('%Y%m%dT%H%M%S%fZ'))
    run.mkdir(parents=True)
    for name in (args.server_jar, 'eula.txt'):
        shutil.copy2(base / name, run / name)
    for name in ('libraries', 'cache', 'versions'):
        shutil.copytree(base / name, run / name)
    plugins = run / 'plugins'
    plugins.mkdir()
    shutil.copy2(plugin, plugins / plugin.name)
    shutil.copy2(probe, plugins / probe.name)
    config = plugins / '3dcasino'
    config.mkdir()
    (run / 'server.properties').write_text(
        f'server-ip=127.0.0.1\nserver-port={args.port}\nonline-mode=false\n'
        'level-name=casino_vanilla_probe\nlevel-type=minecraft:flat\n'
        'generate-structures=false\nallow-nether=false\nspawn-protection=0\n'
        'view-distance=2\nsimulation-distance=2\nenable-rcon=false\n'
        'enable-query=false\nmax-players=1\n', encoding='utf-8')
    result = {'runtime': str(run), 'tested_jar_sha256': hashlib.sha256(plugin.read_bytes()).hexdigest(),
              'server_version': args.server_version, 'server_jar': args.server_jar,
              'server_jar_sha256': hashlib.sha256((base / args.server_jar).read_bytes()).hexdigest(),
              'plugins': sorted(path.name for path in plugins.glob('*.jar')), 'phases': []}
    for phase, marker in enumerate(MARKERS, 1):
        if phase == 2:
            # A stale appearance key must not activate the removed client-pack mode.
            (config / 'config.yml').write_text(
                'menu-enabled: false\nlanguage: custom\nmachine-appearance: resource-pack\n',
                encoding='utf-8')
            (config / 'languages/custom.yml').write_text(
                '"models.showcase_button_play.0": "GO"\n', encoding='utf-8')
        log_path = run / f'phase-{phase}.log'
        with log_path.open('w', encoding='utf-8') as output:
            process = subprocess.run([str(jdk / ('java' + suffix)), '-Xms512M', '-Xmx2G',
                                      '-Dstdout.encoding=UTF-8', '-Dstderr.encoding=UTF-8',
                                      '-Dterminal.jline=false', '-Dterminal.ansi=false',
                                      f'-Dcasino.probe.expected-version={args.server_version}',
                                      '-jar', args.server_jar, 'nogui'], cwd=run,
                                     stdin=subprocess.DEVNULL, stdout=output,
                                     stderr=subprocess.STDOUT, timeout=180, check=False)
        log = log_path.read_text(encoding='utf-8', errors='replace')
        phase_result = {'phase': phase, 'exit_code': process.returncode,
                        'pass': process.returncode == 0 and marker in log
                        and 'CASINO_VANILLA_FAIL' not in log,
                        'evidence': [line for line in log.splitlines() if any(marker in line for marker in
                                     ('CASINO_VANILLA_', 'CASINO_AIM_PASS', 'CASINO_NAMESPACE_TAB_PASS', 'CASINO_FEEDBACK_PASS'))]}
        result['phases'].append(phase_result)
        (run / 'result.json').write_text(json.dumps(result, ensure_ascii=False, indent=2),
                                         encoding='utf-8')
        print(json.dumps(phase_result, ensure_ascii=False, indent=2), flush=True)
        if not phase_result['pass']:
            raise SystemExit(1)


if __name__ == '__main__':
    main()
