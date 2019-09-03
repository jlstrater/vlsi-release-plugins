Checksum Dependency Plugin
==========================

This plugin enables to verify integrity of the used dependencies and Gradle plugins.
In other words, it prevents unexpected use of the patched dependencies.

Unfortunately Gradle (e.g. 5.6) does not verify dependency integrity, and it might easily download and
execute untrusted code (e.g. plugin or dependency). The goal of the plugin is to fill that gap.

See [gradle/issues/5633: save and verify hashes when locking dependencies](https://github.com/gradle/gradle/issues/5633)

Just in case: Maven (e.g. 3.6.0) does not verify dependency integrity, however there's
[pgpverify-maven-plugin](https://github.com/s4u/pgpverify-maven-plugin). It is not clear if
`pgpverify-maven-plugin` can prevent malicious Maven plugins, however it is likely it can not because
Maven configures ALL plugins before start of the execution, so malicious plugins have room
for execution before validation takes place.

Note: if the dependency is signed, it does not mean the file is safe to use. Take your time and
do not trust more dependencies than you should.

Note: the `Checksum Dependency Plugin` validates `artifacts` rather than `dependency tree shape`.
In other words, `Checksum Dependency Plugin` does NOT validate `pom.xml` contents unless
Gradle configuration explicitly adds a dependency on `pom.xml` file.

It is believed that it is a very minimal risk since malicious edits to `pom.xml` would be either
detected (e.g. when an untrusted artifact is added) or they would be harmless (e.g. when
dependency is removed from pom file, or when a new dependency is added and it turns out to be trusted).

Why should I trust Checksum Dependency Plugin?
----------------------------------------------

TL;DR: you should not blindly trust it.

Note: when you integrate `Checksum Dependency Plugin` to your project you need to specify the expected
SHA-512 for the plugin jar itself. That ensures you use "the official" jar file.

The plugin build system produces reproducible builds, so
you can use your own machine to build a jar and verify if it differs.
Extra option is to check Travis builds: SHA-512 checksums are printed there as well.

Checksum Dependency Plugin implements Gradle's [DependencyResolutionListener](https://docs.gradle.org/5.5.1/javadoc/org/gradle/api/artifacts/DependencyResolutionListener.html)
which should transparently all plugins and tasks.

Prior art
---------

https://github.com/signalapp/gradle-witness

The problem with `gradle-witness` is it [cannot verify plugins](https://github.com/signalapp/gradle-witness/issues/10).
Thus `gradle-witness.jar` should be stored in the repository.
`gradle-witness` [does not](https://github.com/signalapp/gradle-witness/issues/24) support [java-library](https://github.com/signalapp/gradle-witness/issues/24)

https://github.com/MatthewDavidBradshaw/Retrial

It seems to be just a rewrite of `gradle-witness` in Kotlin.

`Checksum Dependency Plugin` is probably the first plugin that is able to verify Gradle Plugins and
that is able to use PGP for trust-based verification.

Installation
------------

The plugin is installed via `settings.gradle` (and `buildSrc/settings.gradle` if you have `buildSrc`).

Note: `settings.gradle` is a unusual place for Gradle Plugins, and it enables `Checksum Dependency Plugin`
to capture all the dependency resolutions so the plugin can verify other plugins.

The plugin can be downloaded from Gradle Plugin Portal, or you can add it as a jar file to the project
repository.

Kotlin DSL:

TODD: update samples to match PGP-based implementation

```kotlin
// The below code snippet is provided under CC0 (Public Domain)
// Checksum plugin sources can be validated at https://github.com/vlsi/vlsi-release-plugins
buildscript {
    dependencies {
        classpath("com.github.vlsi.gradle:checksum-dependency-plugin:1.21.0")
        // Alternative option is to use a local jar file via
        // classpath(files("checksum-dependency-plugin-1.21.0.jar"))
        // bouncycastle implements PGP verification
        classpath("org.bouncycastle:bcpg-jdk15on:1.62")
    }
    repositories {
        gradlePluginPortal()
    }
}

// Note: we need to verify the checksum for checksum-dependency-plugin itself
val expectedSha512 = mapOf(
    "43BC9061DFDECA0C421EDF4A76E380413920E788EF01751C81BDC004BD28761FBD4A3F23EA9146ECEDF10C0F85B7BE9A857E9D489A95476525565152E0314B5B"
            to "bcpg-jdk15on-1.62.jar",
    "2BA6A5DEC9C8DAC2EB427A65815EB3A9ADAF4D42D476B136F37CD57E6D013BF4E9140394ABEEA81E42FBDB8FC59228C7B85C549ED294123BF898A7D048B3BD95"
            to "bcprov-jdk15on-1.62.jar",
    "1AA18B47D3F868D60DC0D5418797984B7CE09439181BEEA51DDF6E54D28740412C19FC5A10572C975CC3216EBFE786FD929FF605291B721159FAD9F1DB261F7A"
            to "checksum-dependency-plugin-1.21.0.jar"
)

fun File.sha512(): String {
    val md = java.security.MessageDigest.getInstance("SHA-512")
    forEachBlock { buffer, bytesRead ->
        md.update(buffer, 0, bytesRead)
    }
    return BigInteger(1, md.digest()).toString(16).toUpperCase()
}

val violations =
    buildscript.configurations["classpath"]
        .resolve()
        .sortedBy { it.name }
        .associateWith { it.sha512() }
        .filterNot { (_, sha512) -> expectedSha512.contains(sha512) }
        .entries
        .joinToString("\n  ") { (file, sha512) -> "SHA-512(${file.name}) = $sha512 ($file)" }

if (violations.isNotBlank()) {
    throw GradleException("Buildscript classpath has non-whitelisted files:\n  $violations")
}

apply(plugin = "com.github.vlsi.checksum-dependency")
```

Groovy DSL:

```groovy
// See https://github.com/vlsi/vlsi-release-plugins
buildscript {
  dependencies {
    classpath('com.github.vlsi.gradle:checksum-dependency-plugin:1.21.0')
    // Note: replace with below to use a locally-built jar file
    // classpath(files('checksum-dependency-plugin-1.21.0.jar'))
    classpath("org.bouncycastle:bcpg-jdk15on:1.62")
  }
  repositories {
    gradlePluginPortal()
  }
}

// Note: we need to verify the checksum for checksum-dependency-plugin itself
def expectedSha512 = [
  '43BC9061DFDECA0C421EDF4A76E380413920E788EF01751C81BDC004BD28761FBD4A3F23EA9146ECEDF10C0F85B7BE9A857E9D489A95476525565152E0314B5B':
    'bcpg-jdk15on-1.62.jar',
  '2BA6A5DEC9C8DAC2EB427A65815EB3A9ADAF4D42D476B136F37CD57E6D013BF4E9140394ABEEA81E42FBDB8FC59228C7B85C549ED294123BF898A7D048B3BD95':
    'bcprov-jdk15on-1.62.jar',
  '1AA18B47D3F868D60DC0D5418797984B7CE09439181BEEA51DDF6E54D28740412C19FC5A10572C975CC3216EBFE786FD929FF605291B721159FAD9F1DB261F7A':
    'checksum-dependency-plugin-1.21.0.jar'
]

static def sha512(File file) {
  def md = java.security.MessageDigest.getInstance('SHA-512')
  file.eachByte(8192) { buffer, length ->
     md.update(buffer, 0, length)
  }
  new BigInteger(1, md.digest()).toString(16).toUpperCase()
}

def violations =
  buildscript.configurations.classpath
    .resolve()
    .sort { it.name }
    .collectEntries { [(it): sha512(it)] }
    .findAll { !expectedSha512.containsKey(it.value) }
    .collect { file, sha512 ->  "SHA-512(${file.name}) = $sha512 ($file)" }
    .join("\n  ")

if (!violations.isBlank()) {
    throw new GradleException("Buildscript classpath has non-whitelisted files:\n  $violations")
}

apply plugin: 'com.github.vlsi.checksum-dependency'
```

Configuration
-------------

Checksums and PGP keys for dependency are configured via `checksum.xml`

**Note**: `Checksum Dependency Plugin` can create `checksum.xml` configuration automatically.
You might proceed to `Updating dependencies` section below.

Successful validation requires dependency to be listed at least once (either in `trusted-keys` or in `dependencies`)

```xml
<?xml version='1.0' encoding='utf-8'?>
<dependency-verification version='1'>
    <trust-requirement pgp='GROUP' checksum='NONE' />
    <trusted-keys>
        <trusted-key id='bcf4173966770193' group='org.jetbrains'/>
        <trusted-key id='379ce192d401ab61' group='org.jetbrains.intellij.deps'/>
    </trusted-keys>
    <dependencies>
        <dependency group='com.android.tools' module='dvlib' version='24.0.0'>
            <pgp>ac214caa0612b399</pgp>
            <pgp>bcf4173966770193</pgp>
            <sha512>BF96E53408EAEC8E366F50E0125D6E7E072400887C03EC3C7E8C0B4C9267E5E5B4C0BB2D1FA3355B878DFCEE9334FB145AC38E3CD54D869D9F5283145169DECF</sha512>
            <sha512>239789823479823497823497234978</sha512>
        </dependency>
    </dependencies>
</dependency-verification>
```

It works as follows:

* When dependency has neither dependency-specific configuration nor `trusted-keys`, the artifact would be matched against `<trust-requirement` (==default trust configuration).

    Note: the plugin always requires at last one verification to pass (e.g. checksum or PGP).

    `<trust-requirement pgp='GROUP'` means that artifacts are trusted if they are signed with a `group` key (see "When dependency-specific configuration is missing" below)
    `<trust-requirement pgp='MODULE'` means that artifacts are trusted if they are signed with a `module` key (see "When dependency-specific configuration is present" below)
    `<trust-requirement pgp='NONE'` means that PGP is not verified by default (== it implies that checksums would have to be specified for all the artifacts) 

    `<trust-requirement checksum='NONE'` means that checksums are not  
    `<trust-requirement pgp='NONE'` means that PGP is not verified by default (== it implies that checksums would have to be specified for all the artifacts) 

    For instance, the above configuration has no entries for `com.google.guava:guava:...` because
    `trusted-keys` has no entries for `com.google.guava`, and the only listed `<dependency` is different.

    In that case, `guava` dependency would fail the verification.

    Suggested options:

    * `<trust-requirement pgp='GROUP' checksum='NONE' />` (default) 

        Use `group` PGP keys for verification, and fall back to SHA-512 when no signatures are present.
        This configuration simplifies dependency updates.
 
    * `<trust-requirement pgp='MODULE' checksum='NONE' />` 

        Use `module` PGP keys for verification. Note: as of now, module means `group:artifact:version:classifier@extension`,
        so dependency version update would require to add new section to `checksum.xml`.  

    * `<trust-requirement pgp='MODULE' checksum='MODULE' />` 

        It is the most secure setting, however it might be more complicated to maintain.  
 
* When dependency-specific configuration is missing, `trusted-keys` are used to verify the dependency via `group`

    For instance, `org.jetbrains:annotations:17.0.0` would be accepted if the artifact is signed via key `bcf4173966770193`.
    On the other hand, `org.jetbrains.kotlin:kotlin-bom:1.3.41` would not be accepted because `org.jetbrains.kotlin` group is not listed.

* If dependency-specific configuration is present, then it is used, and `trusted-keys` are ignored

    For instance, `com.android.tools:dvlib:24.0.0` would match if
    the file is `(signed by ac214caa0612b399 or bcf4173966770193) and (SHA-512 is BF96E53408... or 2397898234...)`.

    * If the configuration lists at least one PGP key, then the dependency must be signed by one of the listed keys
    * If the configuration lists at least one checksums, then artifact checksum should be within the listed ones
    * If the configuration lists neither PGP keys nor checksums, then any file would be trusted.

        Note: it is insecure, however that configuration might make sense for in-corporate artifacts
        when PGP signatures are not available and versions are updated often.

Updating dependencies
---------------------

For top security you should alter the configuration manually and ensure you add only trusted dependencies
(see `Configuration`, however avoid recursion :) )

However automatic management of `checksum.xml` would work in most of the cases.

`Checksum Dependency Plugin` saves an updated `checksum.xml` file before the failure, so you can
inspect the changes and apply them if you like.

`allDependencies` task prints all configurations for all projects, so it resolves

    ./gradlew allDependencies
    # The updated checksums can be found in build/checksum/checksum.xml

By default, autogenerated file is placed under `$rootDir/checksum/checksum.xml`, so the file is never
loaded automatically. However if you add `updateChecksum` property (e.g. `-PupdateChecksum`), then
the plugin would overwrite `$rootDir/checksum.xml`.

This is the most automatic (and the least secure option ◔_◔):

    ./gradlew allDependencies -PchecksumUpdate -PchecksumFailOn=build_finish

CI configuration
----------------

The suggested configuration for a CI where untrusted code can not harm (e.g. pull request testing) is

    -PchecksumPrint -PchecksumFailOn=build_finish

Gradle allows to pass properties via environment variables, so you pass the same config via environment variables:

    ORG_GRADLE_PROJECT_checksumPrint=true ORG_GRADLE_PROJECT_checksumFailOn=build_finish

Configuration properties
------------------------

* `checksumUpdate` (bool, default: `false`) updates `checksum.xml` file with new entries

* `checksumPrint` (bool, default: `false`) prints `checksum.xml` to the build log in case there are updates.

    This is suitable for CI environments when you have no access to the filesystem, so you can grab "updated" `checksum.xml`

* `pgpKeyserver` (default: `hkp://hkps.pool.sks-keyservers.net`) specifies keyserver for retrieval of the keys.

    `*.asc` signatures are not sufficient to verify validity, and PGP public keys are required for validation.

* `pgpRetryCount` (default: `10`) specifies the number of attempts to download a PGP key. If the key cannot be downloaded the build is failed.

    The list of retried response codes include: `HTTP_CLIENT_TIMEOUT` (408), `HTTP_INTERNAL_ERROR` (500),
    `HTTP_BAD_GATEWAY` (502), `HTTP_UNAVAILABLE` (503), `HTTP_GATEWAY_TIMEOUT` (504) 

* `pgpInitialRetryDelay` (milliseconds, default: `100`) specifies the initial delay between the retry attempts.

* `pgpMaximumRetryDelay` (milliseconds, default: `10000`) specifies the maximum delay between the retry attempts.

    The delay is increased twice after each failure, so the property sets a cap on the maximum delay.

* `pgpConnectTimeout` (seconds, default: `5`) specifies connect timeout to a PGP server in seconds.

* `pgpReadTimeout` (seconds, default: `20`) specifies timeout for "read PGP key" operation.

Auto-generation mode for `config.xml`: `checksums=optional|mandatory`

Key servers
-----------

PGP keys are resolved via `hkp://hkps.pool.sks-keyservers.net` pool.

It can be configured via `pgpKeyserver` property.

The downloaded keys are cached in the `$rootDir/build/checksum/keystore` directory.

Failure modes
-------------

The mode can be altered via `checksumFailOn` project property (e.g. `-PchecksumFailOn=build_finish`).

* `first_error` (default) is the most secure setting. It prevents untrusted code to be loaded and executed. Basically
dependency verification fails as soon as it detects the first violation.

    The drawback is dependency upgrades might be painful (especially for multi-module projects)
    because it might fail on each newly added dependency "one-by-one", thus it would take time
    to figure out the full set of added dependencies.

* `build_finish` collects all the violations and fails the build at the very end.

    It simplifies development, however it is less secure because it allows execution of untrusted code.

    You might use the following steps to mitigate security issue:
    * Ensure you use HTTPS repositories only
    * Use good dependency hygiene: use well-known dependencies and plugins, avoid doubtful ones.
      Remember that `jar` contents [might vary from the source code](https://blog.autsoft.hu/a-confusing-dependency/)
      that is posted on GitHub.

* `never` collects all the violations, and it does not fail the build.

Verification options
--------------------

* SHA-512 checksum

    SHA-512 is thought to be a strong checksum, so if the downloaded file has matching SHA-512 it means
    the file was not tampered.

    Note: SHA-512 can be generated by anybody, so you must not download SHA-512 checksums from third-party
    servers. You must download SHA-512 only from the sources you trust (e.g. official site).

* PGP signatures

    PGP signatures are hard to tamper, so if PGP signature validation passes, it means that
    the file is signed by the owner of the PGP private key.

    Note: you must not trust `userid` (name, email) that is specified in the PGP key.
    You'd better meet key owner in person and cross-check if they own the key.

Changelog
---------

v1.21.0: PGP verification is implemented