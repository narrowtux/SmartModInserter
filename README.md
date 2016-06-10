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
   
   Keep your mods and factorio up to date.
   
 * Server browser
   
   Add all your favorite servers to a list and have Smart Mod Inserter figure out which mods they use. Then it will install all those mods and launch factorio with a flag to directly connect to that server.
   
   
## Download and Install

To download, head over to the [releases][releases] page. From there, select a release and download the appropriate file.

The `.jar` files are recommended for everyone, however, for OS X users there will be special `.app` bundles available starting with version `0.3`. Additionally to the benefit of having an app-bundle, it will allow you to click the Install links on factoriomods.com.

To run, you need the Java Runtime Environment with version `8 u 40` or higher. Windows users should vary as to not install any unwanted 3rd party software along with Java. To fully prevent installing Ask-Toolbar and the like, usage of [this ninite installer package][java-8-ninite] is recommended.

If you have Java installed, simply opening the `.jar` or `.app` is enough to launch Smart Mod Inserter. 

#### Linux Users

JavaFX is required and may not be installed by default. On Ubuntu, you can fix this by installing these packages:

    sudo apt-get install openjdk-8-jre openjfx libopenjfx-java

### First launch

Upon first launching Smart Mod Inserter, it will probably ask you to define the factorio application path and the data path. On some setups, it will automatically be able to figure out some of those paths. Until you have all necessary paths selected, the close button will stay disabled.

After all paths have been defined, Smart Mod Inserter will move all mods found in `<datadir>/mods/` to `<datadir>/fmm/mods/` and create a modpack titled `Default` containing those mods. You can edit the name if you want.

If you put mods into the `<datadir>/mods/` folder after that, they will also be moved to `<datadir>/fmm/mods/`, if they don't already exist in that folder.

## Compile
To compile, you need at least JDK 1.8_40 and maven.

After executing

        mvn clean package

from the command line, you'll have a .jar file in the `target/` directory.

### OS X
A special build plugin is configured to create an OS X .app bundle. To create this, use maven with the `appbundle:bundle` goal.

## License
This project is licensed under the GNU Lesser General Public License, Version 3

[releases]: https://github.com/narrowtux/FactorioModManager/releases "Releases"
[java-8-ninite]: https://ninite.com/java8/ "Ninite installer for Java 8 without toolbars"
