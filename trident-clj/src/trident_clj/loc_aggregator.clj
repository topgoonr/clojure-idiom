(ns trident-clj.loc-aggregator
  (:import [java.io FileReader]
           [java.util Map Map$Entry List ArrayList Collection Iterator HashMap])
  (:import [storm.trident.operation TridentCollector Function]
           [backtype.storm.tuple Values])
  (:require [clojure.string :as str]
            [clojure.tools.logging :as log])
  (:require [clj-redis.client :as redis])     ; bring in redis namespace
  (:require [trident-clj.redis.redis-datamapper :refer :all])
  (:require [trident-clj.redis.redis-persister :refer :all])
  (:gen-class
    :name com.colorcloud.trident.LocAggregator  ; convert this ns to class Tweet
    :implements [storm.trident.operation.Function]))  ; this ns impl Function


(defn create-tweet-model []
  (def-redis-type tweet-rant
    (string-type :id :name :location :text :time)
    (list-type :followers)
    (primary-key :id :name)
    (format :json)
    (key-separator "##")))


(defn -prepare      ; gen-class method prefix by -
  " called once, better for init global var and db conn "
  [this conf context]
  (prn "LocAggregator prepare once")
  ; init redis data mapper
  (init-redis-db)
  (create-tweet-model))
  ;(def redis-db (redis/init :url "redis://localhost")))  ; shall use dynamic binding

(defn -execute  ; 
  "process each tuple, persist to redis"
  [this ^storm.trident.tuple.TridentTuple tuple ^TridentCollector collector]
  (let [loc (.getString tuple 0)
        tweet (tweet-rant :new)
        idx (rand-int 1000)]
    (prn "TweetAggregator : execute " loc)
    (tweet :set! :id (str idx))
    (tweet :set! :name (str "name-" idx))
    (tweet :set! :location loc)
    (tweet :add! :followers (str "follower-" idx))
    (tweet :add! :followers (str "follower-" (inc idx)))
    (tweet :add! :followers (str "follower-" (dec idx)))
    (tweet :save!)
    (redis/rpush redis-db "tweetloc" loc)
    (.emit collector (Values. (to-array ["test"])))))