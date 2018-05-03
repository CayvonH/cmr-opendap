(ns cmr.opendap.tests.unit.ous.variable
  "Note: this namespace is exclusively for unit tests."
  (:require
    [clojure.test :refer :all]
    [cmr.opendap.ous.variable :as variable]))

; "lat(63.5625,66.09375)"  -> y-lo, y-hi
; "lon(-23.0625,57.09375)"  -> x-lo, x-hi

(deftest lon-lo-phase-shift
  (is (= 0 (variable/lon-lo-phase-shift 360 -180)))
  (is (= 179 (variable/lon-lo-phase-shift 360 0)))
  (is (= 359 (variable/lon-lo-phase-shift 360 180)))
  (is (= 156 (variable/lon-lo-phase-shift 360 -23.0625))))

(deftest lon-hi-phase-shift
  (is (= 0 (variable/lon-hi-phase-shift 360 -180)))
  (is (= 180 (variable/lon-hi-phase-shift 360 0)))
  (is (= 359 (variable/lon-hi-phase-shift 360 180)))
  (is (= 237 (variable/lon-hi-phase-shift 360 57.09375))))

;; XXX Note, lat-lo->y and lat-hi->y get reversed in actual OPeDAP usage;
;;     see the code comment in variable/create-opendap-bounds
(deftest lat-lo-phase-shift
  (is (= 179 (variable/lat-lo-phase-shift 180 -90)))
  (is (= 90 (variable/lat-lo-phase-shift 180 0)))
  (is (= 0 (variable/lat-lo-phase-shift 180 90)))
  (is (= 27 (variable/lat-lo-phase-shift 180 63.5625))))

(deftest lat-hi-phase-shift
  (is (= 179 (variable/lat-hi-phase-shift 180 -90)))
  (is (= 89(variable/lat-hi-phase-shift 180 0)))
  (is (= 0 (variable/lat-hi-phase-shift 180 90)))
  (is (= 23 (variable/lat-hi-phase-shift 180 66.09375))))

(deftest create-opendap-bounds
  (let [dims (variable/map->Dimensions {:x 360 :y 180})
        bounds [-27.421875 53.296875 18.5625 69.75]
        lookup-array (variable/create-opendap-bounds dims bounds)]
    (is (= 152 (get-in lookup-array [:low :x])))
    (is (= 20 (get-in lookup-array [:low :y])))
    (is (= 199 (get-in lookup-array [:high :x])))
    (is (= 37 (get-in lookup-array [:high :y])))))

(deftest format-opendap-bounds
  (let [dims (variable/map->Dimensions {:x 360 :y 180})
        bounds [-9.984375 56.109375 19.828125 67.640625]
        lookup-array (variable/create-opendap-bounds dims bounds)]
    (is (= "MyVar[*][22:34][169:200]"
           (variable/format-opendap-bounds "MyVar" lookup-array))))
  (testing "Bound around Iceland, GB, and Scandanavia ..."
    (let [dims (variable/map->Dimensions {:x 360 :y 180})
          bounds [-27.421875 53.296875 18.5625 69.75]
          lookup-array (variable/create-opendap-bounds dims bounds)]
      (is (= "MyVar[*][20:37][152:199]"
             (variable/format-opendap-bounds "MyVar" lookup-array)))))
  (testing "Narrow band around Icelend stretching to Scandanavia ..."
    (let [dims (variable/map->Dimensions {:x 360 :y 180})
          bounds [-23.0625 63.5625 57.09375 66.09375]
          lookup-array (variable/create-opendap-bounds dims bounds)]
      (is (= "MyVar[*][23:27][156:237]"
             (variable/format-opendap-bounds "MyVar" lookup-array))))))
