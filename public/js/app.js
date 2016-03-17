var mainModule = angular.module('scraper', ['ui.router']);

mainModule.config(function($stateProvider, $urlRouterProvider) {

    $urlRouterProvider.otherwise('/');

    $stateProvider
        .state('home', {
            url: '/',
            templateUrl: '/assets/templates/home.html'
        })

        .state('about', {
            url: '/about',
            templateUrl: '/assets/templates/about.html'
        })
        
        .state('exchanges', {
            url: '/exchanges',
            template: '<exchanges></exchanges>'
        });
});

mainModule.factory('CRUDService', ['$http', function($http) {
    var self = {};
    

    self.get = function(path, params, onSuccess, onFailure) {
        if (angular.isDefined(params))
            $http.get(path, {params: params}).then(onSuccess, onFailure);
        else
            $http.get(path).then(onSuccess, onFailure);
    }

    self.delete = function(path, payload, onSuccess, onFailure) {
        $http.post(path, payload).then(onSuccess, onFailure);
    }
    
    return self;
}]);




