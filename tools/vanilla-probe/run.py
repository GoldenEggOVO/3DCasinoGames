"""Run three clean Purpur phases with only Casino and a vanilla-display probe."""
import argparse
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path
import shutil
import socket
import subprocess
import zipfile

HERE = Path(__file__).resolve().parent
CASINO = HERE.parents[1]
BASE = CASINO.parent / 'table-games-lab/verymcproto-26.2'
JDK = Path('C:/Program Files/Eclipse Adoptium/jdk-25.0.2.10-hotspot/bin')
MARKERS = ('CASINO_VANILLA_FIRST_PASS', 'CASINO_VANILLA_RESTART_PASS',
           'CASINO_VANILLA_DELETE_RESTART_PASS')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--port', type=int, default=25597)
    args = parser.parse_args()
    with socket.socket() as check:
        check.bind(('127.0.0.1', args.port))
    plugin = CASINO / 'target/server-casino-0.5.1-preview.jar'
    classes = CASINO / 'target/vanilla-probe-classes'
    classes.mkdir(parents=True, exist_ok=True)
    jars = [plugin, BASE / 'versions/26.2/purpur-26.2.jar',
            *sorted((BASE / 'libraries').rglob('*.jar'))]
    subprocess.run([str(JDK / 'javac.exe'), '-encoding', 'UTF-8', '-cp',
                    ';'.join(map(str, jars)), '-d', str(classes),
                    str(HERE / 'CasinoVanillaProbe.java')], check=True)
    probe = CASINO / 'target/CasinoVanillaProbe.jar'
    with zipfile.ZipFile(probe, 'w', zipfile.ZIP_DEFLATED) as archive:
        archive.writestr('plugin.yml', 'name: CasinoVanillaProbe\nversion: 1\n'
                         "api-version: '26.2'\nmain: dev.server.casino.probe.CasinoVanillaProbe\n"
                         'depend: [ServerCasino]\n')
        for file in sorted(classes.rglob('*.class')):
            archive.write(file, file.relative_to(classes).as_posix())
    run = CASINO / 'reports/vanilla-runtime' / ('run-' + datetime.now(timezone.utc)
                                                .strftime('%Y%m%dT%H%M%S%fZ'))
    run.mkdir(parents=True)
    for name in ('purpur-2622.jar', 'eula.txt'):
        shutil.copy2(BASE / name, run / name)
    for name in ('libraries', 'cache', 'versions'):
        shutil.copytree(BASE / name, run / name)
    plugins = run / 'plugins'
    plugins.mkdir()
    shutil.copy2(plugin, plugins / plugin.name)
    shutil.copy2(probe, plugins / probe.name)
    config = plugins / 'ServerCasino'
    config.mkdir()
    (config / 'config.yml').write_text('menu-enabled: true\nmachine-appearance: vanilla\n',
                                       encoding='utf-8')
    (run / 'server.properties').write_text(
        f'server-ip=127.0.0.1\nserver-port={args.port}\nonline-mode=false\n'
        'level-name=casino_vanilla_probe\nlevel-type=minecraft:flat\n'
        'generate-structures=false\nallow-nether=false\nspawn-protection=0\n'
        'view-distance=2\nsimulation-distance=2\nenable-rcon=false\n'
        'enable-query=false\nmax-players=1\n', encoding='utf-8')
    result = {'runtime': str(run), 'tested_jar_sha256': hashlib.sha256(plugin.read_bytes()).hexdigest(),
              'plugins': sorted(path.name for path in plugins.glob('*.jar')), 'phases': []}
    for phase, marker in enumerate(MARKERS, 1):
        log_path = run / f'phase-{phase}.log'
        with log_path.open('w', encoding='utf-8') as output:
            process = subprocess.run([str(JDK / 'java.exe'), '-Xms512M', '-Xmx2G',
                                      '-Dstdout.encoding=UTF-8', '-Dstderr.encoding=UTF-8',
                                      '-Dterminal.jline=false', '-Dterminal.ansi=false',
                                      '-jar', 'purpur-2622.jar', 'nogui'], cwd=run,
                                     stdin=subprocess.DEVNULL, stdout=output,
                                     stderr=subprocess.STDOUT, timeout=180, check=False)
        log = log_path.read_text(encoding='utf-8', errors='replace')
        phase_result = {'phase': phase, 'exit_code': process.returncode,
                        'pass': process.returncode == 0 and marker in log
                        and 'CASINO_VANILLA_FAIL' not in log,
                        'evidence': [line for line in log.splitlines() if 'CASINO_VANILLA_' in line]}
        result['phases'].append(phase_result)
        (run / 'result.json').write_text(json.dumps(result, ensure_ascii=False, indent=2),
                                         encoding='utf-8')
        print(json.dumps(phase_result, ensure_ascii=False, indent=2), flush=True)
        if not phase_result['pass']:
            raise SystemExit(1)


if __name__ == '__main__':
    main()
