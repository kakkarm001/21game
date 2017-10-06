/** Controllers */
var clientApp = angular.module('clientApp', ['ngRoute', 'ngWebSocket']);

clientApp.factory('GameDataWSHandler', function($websocket) {

        // Open a WebSocket connection
        var dataStream = $websocket('ws://localhost:9000/chat');

        var collection = [];

        var playerName;

        dataStream.onMessage(function(message) {
          collection.push(JSON.parse(message.data));
        });

        var methods = {
          collection: collection,
          addPlayer: function(playerId) {
            dataStream.send(JSON.stringify({ playerName: playerId }));
          }
        };

        return methods;
   })

  clientApp.controller('joinGameController', function ($scope, GameDataWSHandler, $location, $http) {

     $scope.joinGame = function(){
        console.log("testing ws socket");
        GameDataWSHandler.playerName=$scope.playerName;

         $http.get("http://localhost:9000/createSession")
         .then(function(response) {
               console.log(response.data);
               $location.path("/gameRoom");
          });
        //make websocketConnection
     }

 })

   clientApp.controller('gameController', function ($scope, GameDataWSHandler, $http, $timeout) {

         var BASEURL="http://localhost:9000";

         $scope.gameData=GameDataWSHandler;
         $scope.gameData.addPlayer($scope.gameData.playerName);


        // Function to replicate setInterval using $timeout service.
          function interval(){
              $http.get(BASEURL+"/getGameData")
               .then(function(response) {
                   console.log(response.data);
                   $scope.gameSession = response.data;
                   $scope.btnDisabled=false;
                   $timeout(interval, 10000);
               });
          };

          // Kick off the interval
           interval();

         //button functions
         $scope.deal = function(bet){
            if($scope.gameSession.state=="FIRSTDEAL"){
                $http.get(BASEURL+"/dealCards/"+$scope.gameData.playerName)
                 .then(function(response) {
                     $scope.gameSession = response.data;
                 });
             }
          }

         $scope.userHit = function(bet){
         if($scope.gameSession.state=="ELIMINATION"){
              $scope.btnDisabled=true;
               $http.get(BASEURL+"/hitCard/"+$scope.gameData.playerName+"/"+bet)
               .then(function(response) {
                   console.log(response.data)
               });
           }
        }

         $scope.stand = function(bet){
             if($scope.gameSession.state=="ELIMINATION"){
                   $http.get(BASEURL+"/stand/"+$scope.gameData.playerName+"/"+bet)
                   .then(function(response) {
                       console.log(response.data)
                   });
               }
            }

        $scope.startGame = function(){
           $http.get(BASEURL+"/startGame")
           .then(function(response) {
               $scope.gameSession = response.data;
           });
        }

  })



clientApp.config(function ($routeProvider) {
    $routeProvider
       .when('/game', {
           templateUrl: 'assets/angular/views/joinGame.html',
           controller: 'joinGameController'
       })
       .when('/gameRoom', {
          templateUrl: 'assets/angular/views/gameRoom.html'
         })
       .when('/', {
            templateUrl: 'assets/angular/views/joinGame.html',
            controller: 'joinGameController'
        })
       .otherwise({
           redirectTo: '/'
       });
  })

