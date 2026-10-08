ULike v1.9.52 / Hiro Morphe Patches v1.0.185 publication

The publisher pins published ULike 1.9.51 / bundle 1.0.184, verifies the six freshly generated output assets, and compares the source archive with the exact checkout. Only six reviewed runtime roots and one new finishing GPU library can change. All six existing installer rows, every inherited native library, legacy native methods, and non-ULike application resources remain intact.

The workflow also pins the prior 1.9.50 standalone as an independent host DEX oracle. It is used by inherited H8 regression tests and mutation controls; it is not the release's build baseline. The current baseline must remain 1.9.51 / 1.0.184.

Fresh Mesa GLES/JNI tests verify integer GPU pixels, Java float/mask policy, sequential correction dependencies, 36-row halos, partial-row ownership and failure behavior. Whole-finishing-stage and PerformanceHint tests verify actual reviewed runtime classes and integrated callsites. All retained native libraries must rebuild byte-identically using pinned Android NDK r27c; no old QA record is accepted as evidence that the new implementation ran.

Original ULike 5.6.2 (740) APKS application and Galaxy camera/speed/image-quality testing are not performed by this workflow. QA, release notes, Manager feeds, active policy and the publication receipt state this explicitly. The source archive excludes APKs, photographs and signing keys.

The workflow downloads pinned NDK/JDK/tools and baseline MPPs, rebuilds production DEX/native artifacts, executes host regressions, verifies source hashes and serialised DEX preservation, and finalizes a deterministic source/QA archive. The archived workflow must match the executed workflow. Local and remote read-only preflights run before release publication. The main and dev Manager feeds advance atomically only if their leased heads are still current.

Read-only verification:

    python3 publication/publish1952.py --dist DIST --repo REPO --expected GENERATED_MANIFEST --local-only --baseline INPUT/Hiro_Morphe_Patches_v1.0.184.mpp --standalone-baseline INPUT/ULike_HQ_Texture_Online_v1.9.51.mpp

After publication, update Morphe Manager's patch source and reapply to the unmodified ULike 5.6.2 (740) APKS. A source refresh alone does not update the installed app.
