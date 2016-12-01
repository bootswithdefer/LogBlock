# 1.94

This supports all versions of Bukkit from 1.7.2-R0.3 onwards, including 1.8

## Main changes

* Supports UUIDs - see [this wiki page](https://github.com/LogBlock/LogBlock/wiki/Converting-a-pre-existing-database-to-use-UUIDs) for a short description of the limitations of the migration system
* Supports WorldEdit version 6 and up - unfortunately this means dropping support for previous versions of WorldEdit
* Database insertion optimisation which in tests logged actions six times faster using a local database, and over **250** times faster when the database was on a different server than LogBlock

## Fixes and improvements

* Logs itemdata with values over 256 (logs potion types and item durability)
* Better support for inventory types such as trapped chests
* Fix error when trying to query a double chest without WorldEdit installed
* Correctly log fireballs
* Log players extinguishing fire
* Support utf8mb4 text logging (fixes error when using some obscure characters)
* Overhauled parameter parsing
* Added "/lb hide on" and "/lb hide off" to complement the existing "/lb hide" toggle
* Don't log chat from hidden players
* Fixed error when trying to query illegal triple chests
* Fixed error when a projectile that kills an entity had no source

# 1.80
* Wooden button logging
* Better fireball logging
* Better location querying
* Smarter smart logging
* Add TNT Carts
* Ability to disable sanity ID
* Allow more log rows
* Use UTF-8 for logging
* Comparator, tripwire, pressure plate, crop trample logging added
* Door log fix
* Better messages
* Block spreading
* Fix timezone support
* Death logging fix
* Chest decay
* Various SQL improvements (see source)


# v1.70
* More prepared statements
* Increase to accuracy (Thanks DarkArc)
* WorldEdit logging
* Case insensitive player ignoring
* Consumer warnings + more aggressive defaults (Thanks DarkArc)

# v1.61
* Use more prepared statements
* Fix timestamps on some entries
* Report progress for large rollbacks


# v1.60
* Falling blocks (sand/gravel) are now logged properly in the location they land
* The format of the date displayed in lookups is now customizable under "lookup.dateFormat" in config.yml, please refer to http://docs.oracle.com/javase/1.4.2/docs/api/java/text/SimpleDateFormat.html for the display options
* Parameters can now be escaped with double quotes, allowing for spaces in parameters e.g (world "world with spaces")
* Blocks placed now have their data logged properly. Blocks with extra data such as stairs will now be resetted properly during redos
* Fixed bug whereby lava and water logging options in world config weren't considered
* Fixed duplicate "[LogBlock]" tags in message logging
* Fixed y axis not being considered in the radius specified by area
* Fixed possible crash on /lb tp


# v1.59
* Fix * permission

# v1.58
* Require WorldEdit once again.
* 1.57 table updates are optional

# v1.57
* PermissionDefault to OP
* Increase default table sizes

# v1.56
* Invert chest logging
* Remove worldedit auto download

# v1.55
* Transition to maven
* Use async chat event
* Add metrics
* Small performance increases

# v1.54
* Fix derpy fake inventory plugins throwing npes.

# v1.53
* Make use of the new chest logging system
* Remove the use of old permissions systems

# v1.52
* Properly log block replacement.
* Added option to disallow tool dropping to prevent item duplication.
* Removed double logblock.tp permission.
* Fixed config updater error.
* Check if EntityChangeBlockEvent is triggered by Enderman.
* Fixed onlinetime logging for players.
* Fixed npe in /lb me.
