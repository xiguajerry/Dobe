package dev.exceptionteam.dobe.process

import dev.exceptionteam.dobe.process.transformers.*

object Transformers : Collection<Transformer> by mutableListOf(
    AntiDebugTransformer,
    ShrinkingTransformer,
    KotlinOptimizeTransformer,
    MixinMethodProtectTransformer,
    ScrambleTransformer,
    StringEncryptTransformer,
    NumberEncryptTransformer,
    ControlFlowTransformer,
    NativeCandidateTransformer,
    LocalVariableRenameTransformer,
    BadASMTransformer,
    MethodRenameTransformer,
    FieldRenameTransformer,
    ClassRenameTransformer,
    ShuffleMembersTransformer,
    WatermarkTransformer,
    MiscellaneousObfuscationTransformer,
    TrashClassTransformer,
    InvokeDynamicTransformer,
    TryCatchTransformer
)