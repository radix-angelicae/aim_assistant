

`该项目由AI制作，有很多玄学bug，但不影响正常使用。`


## Features / 功能

### 1. Smart Aim Assist / 智能瞄准

When the aim key is pressed (or toggled), the mod automatically searches for valid targets within the configured range and locks onto the most suitable one based on priority rules.

按下瞄准键（或切换开启后），模组会在配置范围内自动搜索有效目标，并根据优先级规则锁定最合适的目标。

- **Target Priority / 目标优先级**  
  - `DISTANCE` – Closest target first  
  - `ANGLE` – Target closest to crosshair (default)  
  - `HEALTH` – Lowest health first  
  - `RANDOM` – Random selection

- **Lock On Part / 锁定部位**  
  - `HEAD` – Aims at eye level  
  - `BODY` – Aims at the center of hitbox  
  - `FEET` – Aims at feet

- **Smooth Factor / 平滑系数**  
  Controls how quickly the camera rotates toward the target. Lower values = smoother but slower. Range 0.01 – 1.0.  
  控制镜头转向目标的速度。值越低越平滑但越慢，范围 0.01 – 1.0。

- **FOV Angle / 视野角度**  
  Targets outside this angle (from crosshair) are ignored.  
  位于准星此角度之外的目标会被忽略。

- **Distance Weight / 距离权重**  
  Only used when priority is `ANGLE`. Blends distance into angle calculation. Higher weight = closer targets preferred even if slightly off-angle.  
  仅在优先级为 `ANGLE` 时有效。将距离因素混入角度计算中。值越高，即使略微偏离角度，也会优先选择更近的目标。

### 2. Flexible Filter Modes / 灵活过滤模式

Determines which entities are considered valid for aiming and rendering.  
决定哪些实体被认为是有效的（用于瞄准和渲染）。

- `ALL` – All living entities except players.  
  全部（玩家除外）。
- `HOSTILE` – Only monsters (instances of `Monster`).  
  仅敌对生物。
- `ANIMAL` – Only animals (instances of `Animal`).  
  仅动物。
- `SEARCH` – Only entities whose registry ID (e.g. `minecraft:zombie`) matches any pattern in the **Search List**. Supports wildcard `*`.  
  仅检索列表中的实体。支持通配符 `*`，例如 `minecraft:zombie*` 可匹配所有僵尸变种。

> **Note / 注意**  
> In `SEARCH` mode, the search list is also used for line and box colors (`LINE_COLOR_SEARCH` / `BOX_COLOR_SEARCH`), so you can visually distinguish listed targets.  
> 在 `SEARCH` 模式下，检索列表也会决定连线颜色和方框颜色，便于视觉区分。

### 3. Exclusion List / 排除列表

Entities matching any pattern in this list are **completely ignored** – not aimed at, not drawn with lines or boxes, not considered for anything.  
匹配此列表中任意模式的实体将被**完全忽略** – 不瞄准、不绘制连线/方框、不参与任何判定。

- Supports wildcard `*`, e.g. `minecraft:wolf` excludes wolves, `minecraft:*_zombie` excludes all entities ending with `_zombie`.  
  支持通配符 `*`，例如 `minecraft:wolf` 排除狼，`minecraft:*_zombie` 排除所有以 `_zombie` 结尾的实体。

- Exclusion list is applied **after** the filter mode. If an entity passes the filter but is excluded, it still won’t be targeted or rendered.  
  排除列表在过滤模式**之后**应用。如果实体通过了过滤器但被排除，仍然不会成为目标或被渲染。

### 4. Visual Rendering / 视觉渲染

All rendering is independent of aiming and can be toggled on/off with the **Persistent Render** key binding.  
所有渲染独立于瞄准功能，可通过**持续渲染开关**按键单独开启/关闭。

#### Lines (from crosshair point to entity) / 连线（从准星点到实体）
- Color configurable by entity type (Player / Monster / Animal / Other / Search).  
  颜色按实体类型独立配置（玩家/敌对/动物/其他/检索）。
- Line width adjustable.  
  线宽可调。

#### Bounding Boxes / 包围盒
- **Box Mode / 方框模式**  
  - `FULL_3D` – Complete wireframe cube.  
    完整立方体线框。  
  - `CORNERS` – Only 3 small lines at each corner.  
    仅每个角上的三根短边（角框）。  
  - `BOTTOM` – Only the bottom face.  
    仅底面。  
  - `TOP` – Only the top face.  
    仅顶面。
- **Box Scale / 框大小缩放**  
  Adjusts the bounding box size relative to the entity’s actual hitbox. Useful for making boxes slightly larger or smaller than the model.  
  调整包围盒相对于实体实际碰撞箱的大小。可用于让框比模型稍大或稍小。
- **Box Alpha / 方框透明度** – 0 = fully transparent, 1 = fully opaque.  
  0 = 完全透明，1 = 完全不透明。

#### Text Labels / 文字标签
- Displays floating text above entities.  
  在实体上方显示悬浮文字。
- Configurable fields: Name, Health (❤), Armor (🛡), Distance, Entity Type.  
  可配置字段：名称、血量（❤）、护甲（🛡）、距离、实体类型。
- **Text Scale / 文字缩放** – Because 3D world scale differs from screen scale, values are typically very small (default 0.025). Increase to make text larger.  
  因 3D 世界尺度与屏幕尺度不同，该值通常很小（默认 0.025）。增大可使文字变大。
- **Text Max Distance / 文字最大距离** – Labels beyond this range are not rendered.  
  超出此距离的标签不渲染。
- **Text Follow Filter / 标签跟随过滤器**  
  - ON: Only shows labels for entities that pass the current filter mode.  
    开启：仅对通过当前过滤器的实体显示标签。  
  - OFF: Shows labels for **all** living entities (except excluded ones).  
    关闭：对**所有**生物显示标签（排除列表中的除外）。

#### Target Glow / 目标发光
- When a target is locked, a glowing outline (with configurable color) highlights it.  
  锁定目标时，目标会显示发光轮廓（颜色可配置）。

### 5. Auto Attack / 自动攻击

When enabled, the mod will automatically attack the locked target once the player’s attack strength (weapon cooldown) reaches 100% and the target is within reach.  
启用后，当玩家的攻击强度（武器冷却）达到 100% 且目标在触及范围内时，模组会自动攻击锁定的目标。

- Uses `Minecraft.gameMode.attack()` – same as left-clicking.  
  使用 `Minecraft.gameMode.attack()` – 等同于左键攻击。
- Does not bypass any game mechanics (reach limit, invincibility frames, etc.).  
  不会绕过任何游戏机制（触及距离限制、无敌帧等）。

### 6. Wall Penetration & Invisibility / 穿墙 & 隐身

- **Allow Wall Penetration / 允许穿墙锁定**  
  OFF: Aiming requires a clear line of sight (raycast to target’s eye position).  
  关闭：瞄准需要视线无阻挡（射线检测到目标眼睛位置）。  
  ON: Line of sight is ignored.  
  开启：忽略视线阻挡。

- **Ignore Invisible / 忽略隐身**  
  ON: Invisible entities (e.g. from potion effects) are never targeted.  
  开启：永远不会瞄准隐身实体（如药水效果）。

### 7. Aim Key Modes / 瞄准按键模式

- `HOLD` – Aiming is active only while the key is pressed down.  
  按住 – 仅当按键按住时瞄准激活。
- `TOGGLE` – Press once to enable aiming, press again to disable.  
  切换 – 按一次启用瞄准，再按一次禁用。

### 8. Target Switching / 目标切换

- **Allow Target Switching / 允许目标切换**  
  ON: The mod continuously re-evaluates the best target and can switch to a better one.  
  开启：模组持续重新评估最佳目标，可切换到更优目标。  
  OFF: Once locked, the target will not change until it dies, goes out of range, or becomes invalid.  
  关闭：锁定后，目标不会改变，直到其死亡、超出范围或变为无效。

- A dedicated key binding `Toggle Target Switch` can turn this on/off in-game without opening the config screen.  
  专用按键绑定 `Toggle Target Switch` 可在游戏中开关此功能，无需打开配置界面。

### 9. HUD Info & Panel / 界面信息

- **Simple Aim Info / 简单信息** – One line showing current target’s name, health, and distance. Position adjustable by percentage from top-left.  
  简单信息 – 一行文字显示当前目标的名称、血量、距离。位置可调（基于左上角的百分比）。
- **Aim Panel / 信息面板** – A multi-line panel (position fixed at top-left) showing detailed fields: name, health, armor, distance, type. Each field can be toggled individually.  
  信息面板 – 多行面板（位置固定在左上角），显示详细字段：名称、血量、护甲、距离、类型。每个字段可单独开关。

### 10. Configuration Screen / 配置界面

A full GUI to adjust every setting:  
完整的图形界面，可调整所有设置：

- Numeric fields with range validation  
  带范围验证的数字输入框
- Color pickers (hex input + live preview swatch)  
  颜色选择器（十六进制输入 + 实时预览色块）
- Entity list editors with wildcard suggestions (auto-completes from registered entity types)  
  实体列表编辑器，带通配符建议（自动补全已注册的实体类型）
- Scrolling for long lists and content  
  长列表和内容支持滚动

Press the configured **Open Config** key to open it. All changes are saved when you close the screen.  
按下配置的 **打开配置界面** 键打开。关闭界面时自动保存所有更改。
