$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
$resources=Join-Path $root 'src/main/resources'
$java=Join-Path $root 'src/main/java/com/github/saku0817/combatcoresystems'
$out=Join-Path $root 'docs/ChatGPT-YAML-Reference.md'
$lines=[System.Collections.Generic.List[string]]::new()
$lines.Add('# CombatCoreSystems v1.4.6 — YAML・実装参照資料')
$lines.Add('')
$lines.Add('この資料は配布ソースの既定YAMLと、設定を解釈する主なJava実装を機械的に収録しています。生成時は引き継ぎ書・ChatGPT-YAML-v1.4.6.mdも参照してください。運用中の秘密情報は含めないでください。')
$lines.Add('')
foreach($file in Get-ChildItem -LiteralPath $resources -Filter '*.yml' | Where-Object Name -ne 'plugin.yml' | Sort-Object Name){
    $lines.Add("## $($file.Name)");$lines.Add('');$lines.Add('```yaml')
    $lines.Add((Get-Content -LiteralPath $file.FullName -Raw -Encoding UTF8).TrimEnd())
    $lines.Add('```');$lines.Add('')
}
$sources=@(
    'config/DefinitionRegistry.java',
    'config/WeaponOptionsParser.java',
    'config/TriggerCatalog.java',
    'model/StatKey.java',
    'model/EquipmentRolls.java',
    'model/EquipmentGrowthTable.java',
    'model/EquipmentGrowth.java',
    'model/ItemInstance.java',
    'model/trigger/Area.java',
    'model/trigger/EventContext.java',
    'model/trigger/EventModifier.java',
    'model/trigger/CombatModifiers.java',
    'model/trigger/MovementPath.java',
    'model/trigger/SourceKind.java',
    'model/trigger/TriggerChain.java',
    'model/trigger/TriggerDefinition.java',
    'model/trigger/TriggerEvent.java',
    'service/TriggerService.java',
    'service/StackService.java',
    'service/TargetSelectorService.java',
    'service/FieldService.java',
    'service/DynamicEffectService.java',
    'service/CombatModifierService.java',
    'service/MovementService.java',
    'service/HealService.java',
    'service/BuffService.java',
    'service/DamageService.java',
    'service/SkillService.java',
    'service/StatService.java'
)
foreach($relative in $sources){
    $file=Join-Path $java $relative
    if(-not(Test-Path -LiteralPath $file)){throw "Missing reference source: $file"}
    $lines.Add("## $([System.IO.Path]::GetFileName($file))");$lines.Add('');$lines.Add('ソース: `src/main/java/com/github/saku0817/combatcoresystems/' + $relative + '`');$lines.Add('');$lines.Add('```java')
    $lines.Add((Get-Content -LiteralPath $file -Raw -Encoding UTF8).TrimEnd())
    $lines.Add('```');$lines.Add('')
}
[System.IO.File]::WriteAllText($out,($lines -join "`n"),[System.Text.UTF8Encoding]::new($false))
Write-Output "Generated: $out"
