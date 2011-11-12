# Twitter Bootstrap

Bootstrap is included via a git submodule. After creating this project
you must run:

    git init
    git submodule add https://github.com/twitter/bootstrap.git modules/bootstrap
    git submodule init
    git add modules/bootstrap

This will pull in the necessary files.

Once the submodule files (modules/bootstrap and .gitmodules) have been committed to the repository,
it can be cloned regularly, but you must run:

    git submodule update

For the files to be pulled in.

To update the version that is being used, you must run _git fetch/pull_ in the submodule
directory, then commit the version the submodule is using to the main project. For
more information, see:

[Understanding Git Submodules](http://speirs.org/blog/2009/5/11/understanding-git-submodules.html)

# MongoDB

This app uses MongoDB. Therefore, you will need to either have it installed locally, or use one of
the cloud providers and configure it in your props file. See config.MongoConfig for more info.

# Building

This app uses sbt 0.11. To build for the first time, run:

    bash$ sbt
    > compile
    > container:start

It will be running on http://localhost:8080

# User  Model

This app implements the [Mongoauth Lift Module](https://github.com/eltimn/lift-mongoauth).
The registration and login implementation is based on
[research done by Google](http://sites.google.com/site/oauthgoog/UXFedLogin) a few years ago
and is similar to Amazon.com and Buy.com. It's different than what most people seem to expect,
but it can easily be changed to suit your needs since most of the code is part of your project.

# CloudFoundry

To Push to CloudFoundry, you must specify a path so it can find the war:

    vmc update beamstream --path target/scala_2.9.1/

There is a _cfdeploy_ task defined in sbt that will do this for you. It will only work
after an intial _vmc push_.

# After Pre Beta mode

* Delete all documents in user.extsessions
* Send LoginToken to all users that had signed up via email only
* Send link to home page for users that signed up via facebook. They will need to click the connect with facebook button.
