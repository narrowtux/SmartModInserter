# Smart Mod Inserter
This is a smart mod manager for factorio. It will allow you to create modpacks and later launch factorio with the mods of those modpacks with a single click. 

## Features
An incomplete list of features. Features marked with a `*` are only available in the indev version.

 * Modpacks
   
   Define multiple modpacks and switch between them with a single click.

 * Dependency resolving `*`
   
   If you have a modpack and you didn't include all the dependencies in the modpack, this application will try to solve that problem.
 * Savegames `*`
   
   You can select any save game from the `Saves` tab and hit Play. This will install all the mods that are listed as required in this save.
 * Download mods `*`
   
   If you want to download a mod from factoriomods.com, you can do so by clicking on the `Install with FMM` button on that website. This will download the mod and store it under the `Mods` tab.
   All other websites are welcome to implement the `factoriomods` URI scheme
   
## Future features

 * Factorio version management
   
   You'll be able to define more than one factorio version. Dependency resolving will then automatically determine the correct factorio version for each modpack.
   
 * Automatic updates
   
   Keep all your mods up to date.
   
 * Server browser
   
   Add all your favorite servers to a list and have Smart Mod Inserter figure out which mods they use. Then it will install all those mods and launch factorio with a flag to directly connect to that server.

## Compile
To compile, you need at least JDK 1.8_40 and maven.

After executing

        mvn clean package

from the command line, you'll have a .jar file in the `target/` directory.

### OS X
A special build plugin is configured to create an OS X .app bundle. To create this, use maven with the `appbundle:bundle` goal.

## License
This project is licensed under the GNU Lesser General Public License, Version 3