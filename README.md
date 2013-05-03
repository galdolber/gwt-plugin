# gwt-plugin

A Leiningen plugin run and build gwt applications.

## Usage

Put `[gwt-plugin "0.1.0"]` into the `:plugins` vector of your project.clj.

## Resources task

Copies all resources to the compile-path and optionally copies web resources to the web-path.

Sample web resources configuration (to add in your project.clj):
  `:web-resource-paths ["src/main/webapp"]`
  `:web-path "target/server/static"`

    $ lein resources

## Three rounds java compiler

This task is like javac but runs 3 times(incrementally) to support annotations processors.
Only shows errors thrown in the last compilation.

    $ lein javacc

## Gwt tasks

Sample configuration (to add in your project.clj):
  `:gwt {:module "com.your.Module"
         :localWorkers 1
         :war "path/to/war/folder"
         :deploy "path/to/extra/folder"
         :noserver false
         :extraJvmArgs "-XX:MaxPermSize=512m"}`

    $ lein gwt compile
    $ lein gwt run

## License

The use and distribution terms for this software are covered by the
Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
which can be found in the file LICENSE.html at the root of this distribution.
By using this software in any fashion, you are agreeing to be bound by
the terms of this license.
You must not remove this notice, or any other, from this software.
