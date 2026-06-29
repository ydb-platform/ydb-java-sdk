# YDB Java SDK Versioning Policy

By adhering to these guidelines and exceptions, we aim to provide a stable and reliable development experience for our users (aka [LTS](https://en.wikipedia.org/wiki/Long-term_support)) while still allowing for innovation and improvement.

We endeavor to adhere to versioning guidelines as defined by [SemVer2.0.0](https://semver.org/).

We making the following exceptions to those guidelines:
## Experimental
   - We use the `@Experimental` annotation for new features in the `ydb-java-sdk`.
   - Early adopters of newest feature can report bugs and imperfections in functionality.
   - For fix this issues we can make broken changes in experimental API.
   - We reserve the right to remove or modify these experimental features at any time without increase of major part of version.
   - We want to make experimental API as stable in the future
## Deprecated
   - We use the `@Deprecated` annotation for deprecated features in the `ydb-java-sdk`.
   - This helps to our users to soft decline to use the deprecated feature without any impact on their code.
   - Deprecated features will not be removed or changed for a minimum period of **six months** since the mark added.
   - We reserve the right to remove or modify these deprecated features without increase of major part of version.
## Internals
   - Some public API of `ydb-java-sdk` relate to the internals.
   - Internal classes are located in packages `impl` or `utils`.
   - `ydb-java-sdk` internals can be changed at any time without increase of major part of version.
   - Internals will never marked as stable
