[Spigot]: https://www.spigotmc.org/resources/46404/
[Jenkins]: https://ci.mg-dev.eu/job/Maplands/
[BKCommonLib]: https://www.spigotmc.org/resources/39590/
[issues]: https://github.com/bergerhealer/Maplands/issues

[imgPreview]: https://user-images.githubusercontent.com/11576465/84956541-bb10b100-b0f9-11ea-95ba-0de01ea225cb.png
[imgControl]: https://user-images.githubusercontent.com/11576465/84956444-843a9b00-b0f9-11ea-9e2f-81d478f170d1.png

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
