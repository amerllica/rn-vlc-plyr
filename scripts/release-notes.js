const fs = require('node:fs');
const path = require('node:path');

const REPOSITORY_URL = 'https://github.com/amerllica/rn-vlc-plyr';
const NPM_URL = 'https://www.npmjs.com/package/rn-vlc-plyr';
const UNRELEASED = 'Unreleased';
const VERSION_HEADING = /^## \[([^\]]+)\]/;
const LINK_REFERENCE = /^\[[^\]]+\]: /;
const RELATIVE_LINK = /\]\((?!https?:|mailto:|#)([^)]+)\)/g;

const isPrerelease = (version) => version.includes('-');

function splitSections(changelog) {
  const sections = [];
  for (const line of changelog.split('\n')) {
    const heading = line.match(VERSION_HEADING);
    if (heading) {
      sections.push({ version: heading[1], lines: [] });
    } else if (sections.length > 0 && !LINK_REFERENCE.test(line)) {
      sections[sections.length - 1].lines.push(line);
    }
  }
  return sections;
}

function dropEmptySubsections(lines) {
  return lines.filter((line, index) => {
    if (!line.startsWith('### ')) {
      return true;
    }
    const next = lines
      .slice(index + 1)
      .find((candidate) => candidate.trim() !== '');
    return next !== undefined && !next.startsWith('### ');
  });
}

function findSection(sections, version) {
  const exact = sections.findIndex((section) => section.version === version);
  if (exact !== -1) {
    return { index: exact, prerelease: isPrerelease(version) };
  }
  const unreleased = sections.findIndex(
    (section) => section.version === UNRELEASED
  );
  if (isPrerelease(version) && unreleased !== -1) {
    return { index: unreleased, prerelease: true };
  }
  throw new Error(
    `CHANGELOG.md has no "## [${version}]" section. Add one before you tag v${version}.`
  );
}

function previousRelease(sections, index) {
  return sections
    .slice(index + 1)
    .find(
      (section) =>
        section.version !== UNRELEASED && !isPrerelease(section.version)
    )?.version;
}

function fullChangelogUrl(version, previous) {
  return previous
    ? `${REPOSITORY_URL}/compare/v${previous}...v${version}`
    : `${REPOSITORY_URL}/commits/v${version}`;
}

function readEngines({ podspec, gradle, packageJson }) {
  const ios = podspec.match(/dependency\s+"MobileVLCKit",\s*"([^"]+)"/)?.[1];
  const android = gradle.match(/libvlc-all:([\w.-]+)/)?.[1];
  const nitro = packageJson.peerDependencies?.['react-native-nitro-modules'];
  if (!ios || !android || !nitro) {
    throw new Error(
      'Could not read the MobileVLCKit, libvlc-android or Nitro Modules version.'
    );
  }
  return `MobileVLCKit ${ios}, libvlc-android ${android}, Nitro Modules ${nitro}`;
}

function buildReleaseNotes({ changelog, version, engines }) {
  const sections = splitSections(changelog);
  const { index, prerelease } = findSection(sections, version);
  const body = dropEmptySubsections(sections[index].lines)
    .join('\n')
    .replace(RELATIVE_LINK, `](${REPOSITORY_URL}/blob/v${version}/$1)`)
    .trim();
  if (body === '') {
    throw new Error(`The CHANGELOG.md section for ${version} is empty.`);
  }
  const notice = prerelease
    ? ['> **Pre-release.** It is published under the `beta` npm tag.', '']
    : [];
  return [
    ...notice,
    body,
    '',
    '---',
    '',
    `- **Install:** \`yarn add rn-vlc-plyr@${version}\``,
    `- **npm:** ${NPM_URL}/v/${version}`,
    `- **Native engines:** ${engines}`,
    `- **Full Changelog:** ${fullChangelogUrl(version, previousRelease(sections, index))}`,
    '',
  ].join('\n');
}

function main() {
  const root = path.resolve(__dirname, '..');
  const read = (file) => fs.readFileSync(path.join(root, file), 'utf8');
  const packageJson = JSON.parse(read('package.json'));
  try {
    process.stdout.write(
      buildReleaseNotes({
        changelog: read('CHANGELOG.md'),
        version: process.argv[2] ?? packageJson.version,
        engines: readEngines({
          podspec: read('RnVlcPlyr.podspec'),
          gradle: read('android/build.gradle'),
          packageJson,
        }),
      })
    );
  } catch (error) {
    console.error(error.message);
    process.exitCode = 1;
  }
}

if (require.main === module) {
  main();
}

module.exports = { buildReleaseNotes, readEngines };
