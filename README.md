[Spigot]: https://www.spigotmc.org/resources/46404/
[Jenkins]: https://ci.mg-dev.eu/job/Maplands/
[BKCommonLib]: https://www.spigotmc.org/resources/39590/
[issues]: https://github.com/bergerhealer/Maplands/issues

[imgPreview]: https://www.spigotmc.org/attachments/maplands_demo-jpg.277704/
[imgControl]: https://www.spigotmc.org/attachments/held_map_ui-png.279691/

# Maplands
[Spigot page][spigot] | [Dev Builds][Jenkins]

Minecraft worlds in the map dimension

## About
Maplands is a work-in-progress project delivering world map rendering to Minecraft maps. Display the world in an isometric view, link maps together and create a huge overview of your server, and much more is anticipated!

You will need the latest build of [BKCommonLib] for this plugin to work correctly.

Please report bugs and ask for feature requests on the [issue page][issues]

![preview][imgPreview]

## Usage
Use `/map` to give yourself a map from maplands. You need the permission `maplands.command.map` to use this command.

### Controls
While holding the map...
- Use W, A, S or D to move the position on the map.
  - If you want to move while still holding the map, swap it to your offhand.
- Sneak (Press shift) to enter or leave the menu.
  - While in the menu use A and D to switch between the options.
  - Jump (Press Space) to select an option.
- Pressing Jump while not in the menu re-renders the entire map.

![control][imgControl]
