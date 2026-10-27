const { buildReleaseNotes, readEngines } = require('../release-notes');

const ENGINES =
  'MobileVLCKit ~> 3.7.4, libvlc-android 3.7.7, Nitro Modules >=0.37.0';

const CHANGELOG = `# Changelog

## [Unreleased]

### Added

- Next thing.

## [1.1.0] - 2026-10-20

### Highlights

Background audio.

### Added

- \`nowPlaying\`. See [ADR 0018](docs/adr/0018-media-session-stack.md).

### Fixed

### Docs

- [Website](https://example.com) and [top](#top).

## [1.0.1-beta.1] - 2026-10-08

### Fixed

- Beta fix.

## [1.0.0] - 2026-10-07

### Added

- First release.

## [0.9.0] - 2026-10-01

[Unreleased]: https://github.com/amerllica/rn-vlc-plyr/compare/v1.1.0...HEAD
[1.1.0]: https://github.com/amerllica/rn-vlc-plyr/compare/v1.0.0...v1.1.0
`;

const notesFor = (version, changelog = CHANGELOG) =>
  buildReleaseNotes({ changelog, version, engines: ENGINES });

describe('buildReleaseNotes', () => {
  it('returns the section body without its heading', () => {
    const notes = notesFor('1.1.0');

    expect(notes.startsWith('### Highlights\n\nBackground audio.')).toBe(true);
    expect(notes).not.toContain('## [1.1.0]');
    expect(notes).not.toContain('Next thing.');
    expect(notes).not.toContain('Beta fix.');
  });

  it('drops subsections that have no lines', () => {
    const notes = notesFor('1.1.0');

    expect(notes).not.toContain('### Fixed');
    expect(notes).toContain('### Docs');
  });

  it('turns relative links into links to the tagged file', () => {
    expect(notesFor('1.1.0')).toContain(
      '[ADR 0018](https://github.com/amerllica/rn-vlc-plyr/blob/v1.1.0/docs/adr/0018-media-session-stack.md)'
    );
  });

  it('keeps absolute and anchor links', () => {
    expect(notesFor('1.1.0')).toContain(
      '[Website](https://example.com) and [top](#top).'
    );
  });

  it('leaves out the link references at the end of the file', () => {
    expect(notesFor('1.0.0')).not.toContain('[Unreleased]:');
  });

  it('adds the install, npm, engines and full changelog footer', () => {
    expect(notesFor('1.1.0')).toContain(
      [
        '---',
        '',
        '- **Install:** `yarn add rn-vlc-plyr@1.1.0`',
        '- **npm:** https://www.npmjs.com/package/rn-vlc-plyr/v/1.1.0',
        `- **Native engines:** ${ENGINES}`,
        '- **Full Changelog:** https://github.com/amerllica/rn-vlc-plyr/compare/v1.0.0...v1.1.0',
      ].join('\n')
    );
  });

  it('compares against the previous stable release, skipping betas', () => {
    expect(notesFor('1.1.0')).toContain('compare/v1.0.0...v1.1.0');
  });

  it('links the commits of the tag for the first release', () => {
    const changelog =
      '## [1.0.0] - 2026-10-07\n\n### Added\n\n- First release.\n';

    expect(notesFor('1.0.0', changelog)).toContain(
      '- **Full Changelog:** https://github.com/amerllica/rn-vlc-plyr/commits/v1.0.0'
    );
  });

  it('marks a beta section as a pre-release', () => {
    const notes = notesFor('1.0.1-beta.1');

    expect(notes.startsWith('> **Pre-release.**')).toBe(true);
    expect(notes).toContain('- Beta fix.');
    expect(notes).toContain('compare/v1.0.0...v1.0.1-beta.1');
  });

  it('uses the Unreleased section for a beta that has no section', () => {
    const notes = notesFor('1.2.0-beta.1');

    expect(notes).toContain('- Next thing.');
    expect(notes).toContain('> **Pre-release.**');
    expect(notes).toContain('compare/v1.1.0...v1.2.0-beta.1');
  });

  it('fails for a stable version that has no section', () => {
    expect(() => notesFor('9.9.9')).toThrow(
      'CHANGELOG.md has no "## [9.9.9]" section'
    );
  });

  it('fails for a section with no body', () => {
    expect(() => notesFor('0.9.0')).toThrow(
      'The CHANGELOG.md section for 0.9.0 is empty.'
    );
  });
});

describe('readEngines', () => {
  const sources = {
    podspec: '  s.dependency "MobileVLCKit", "~> 3.7.4"\n',
    gradle: '  implementation "org.videolan.android:libvlc-all:3.7.7"\n',
    packageJson: {
      peerDependencies: { 'react-native-nitro-modules': '>=0.37.0' },
    },
  };

  it('reads the engine versions from the podspec, Gradle and package.json', () => {
    expect(readEngines(sources)).toBe(ENGINES);
  });

  it('fails when a version is missing', () => {
    expect(() => readEngines({ ...sources, gradle: '' })).toThrow(
      'Could not read'
    );
  });
});
