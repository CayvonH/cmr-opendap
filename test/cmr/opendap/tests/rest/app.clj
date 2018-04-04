(ns ^{:integration true} cmr.opendap.tests.rest.app
  "Note: this namespace is exclusively for integration tests; all tests defined
  here will use one or more integration test fixtures."
  (:require
    [clj-http.client :as httpc]
    [clojure.test :refer :all]
    [cmr.opendap.testing.system :as test-system])
  (:import
    (clojure.lang ExceptionInfo)))

(use-fixtures :once test-system/with-system)

(deftest admin-routes
  (testing "ping routes ..."
    (let [result (httpc/get (format "http://localhost:%s/ping"
                                    (test-system/http-port))
                            {:as :json})]
      (is (= 200 (:status result)))
      (is (= "pong" (get-in result [:body :result]))))
    (let [result (httpc/post (format "http://localhost:%s/ping"
                                     (test-system/http-port))
                             {:as :json})]
      (is (= 200 (:status result)))
      (is (= "pong" (get-in result [:body :result])))))
  (testing "health route ..."
    (let [result (httpc/get (format "http://localhost:%s/health"
                                    (test-system/http-port))
                            {:as :json})]
      (is (= 200 (:status result)))
      (is (= {:config {:ok? true}
              :httpd {:ok? true}
              :logging {:ok? false}}
             (:body result))))))

(deftest testing-routes
  (is (thrown-with-msg? ExceptionInfo #"status 401"
    (httpc/get (format "http://localhost:%s/testing/401"
                       (test-system/http-port)))))
  (is (thrown-with-msg? ExceptionInfo #"status 403"
    (httpc/get (format "http://localhost:%s/testing/403"
                       (test-system/http-port)))))
  (is (thrown-with-msg? ExceptionInfo #"status 404"
    (httpc/get (format "http://localhost:%s/testing/404"
                       (test-system/http-port)))))
  (is (thrown-with-msg? ExceptionInfo #"status 405"
    (httpc/get (format "http://localhost:%s/testing/405"
                       (test-system/http-port)))))
  (is (thrown-with-msg? ExceptionInfo #"status 500"
    (httpc/get (format "http://localhost:%s/testing/500"
                       (test-system/http-port)))))
  (is (thrown-with-msg? ExceptionInfo #"status 503"
    (httpc/get (format "http://localhost:%s/testing/503"
                       (test-system/http-port))))))
