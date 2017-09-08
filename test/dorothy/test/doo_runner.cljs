(ns dorothy.test.doo-runner
  (:require [doo.runner :refer-macros [doo-tests]]

            dorothy.test.core))

(doo-tests 'dorothy.test.core)