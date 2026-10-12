# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
### Added
- Embedded `LocalHttpServer` running on ephemeral loopback (`127.0.0.1`) to serve local HTML, CSS, JS, and 3D assets.

### Fixed
- Fixed `ERR_FAILED` and macOS signature/code-signing validation errors when serving local files via custom protocol schemes in JCEF.
- Transparent rewriting of legacy `http://webtoolkit/__webtk_api.js` references to `/__webtk_api.js` for backwards compatibility.
- Fixed path resolution handling double slashes and absolute paths across Windows, macOS, and Linux.

## [0.1.0] - 2026-08-26

### Added
- Initial release of AnyLogic Web Toolkit.
- Bidirectional Java ↔ JavaScript communication bridge using JCEF.
- File system access API (open/save dialogs, read/write files).
- State synchronization key-value store.
- Custom `http://webtoolkit/` scheme handler for loading local assets.
- Integrated `AnyLogic.call()` and `AnyLogic.events` JavaScript API.
- Three example AnyLogic models: BasicExample, BidirectionalExample, and LogisticsDigitalTwin.
