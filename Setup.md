# First Elevation Aware Router - feveR

This is a simple guide to run feveR on vagrant. Once the repo has been cloned move to the feveR directory and you can start with the following instructions. 

### Copy default config file into graphhopper/

```sh
$ cd config/config.properties graphhopper/
```

### Download PBF

There exist many hoster that provide pbf files of parts of OSM. A very convenient one is: [BBBike].

e.g. Pbf file for Innsbruck: [BBBike IBK]

Download the file and place it in the graphhopper/ directory.


### Vagrant

```sh
$ vagrant up
$ vagrant ssh
$ cd /vagrant/graphhopper/
```

### Start feveR

```sh
$ ./graphhopper.sh web Innsbruck.osm.pbf
```


### Use feveR

Open browser: http://192.168.5.2:8989
Or click here: [feveR]

* Select map visualization on the top right corner
* Click on bike button top left corner to use feveR routing
* Enter start and end points
* Click on search button
* Right lower corner visualizes elevation profile


### Using and creating profiles

Up to now it is only possible to create, delete and add tracks to a profile through the command line on the server.

Creating a new profile:

```sh
$ ./profiles.sh create <profile>
```

Add track to profile

```sh
$ ./profiles.sh add <profile> <track> <pbf-file>
```

Searching a route for this profile:

* Add parameter to url: &profile=<profile>

[BBBike]: <http://download.bbbike.org/osm/>
[BBBike IBK]: <http://download.bbbike.org/osm/bbbike/Innsbruck/>
[feveR]: <http://192.168.5.2:8989>