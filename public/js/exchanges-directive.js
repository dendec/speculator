(function(){

    this.tableController = ["$scope", "CRUDService", function($scope, CRUDService) {

        $scope.exchanges = [];
        $scope.selectedExchange = {};
        $scope.exchangeNames = [];
        $scope.selectedExchangeName = "";
        $scope.points = [];
        $scope.chartTypes = ["chart", "candlesticks", "sma"];
        $scope.selectedChartType = $scope.chartTypes[0];
        $scope.drawingMethod = "chart";
        $scope.durations = [{name: "1 min", value: 1},{name: "3 min", value: 3},{name: "5 min", value: 5},{name: "15 min", value: 15},{name: "30 min", value: 30},{name: "1 hour", value: 60},{name: "2 hours", value: 120}];
        $scope.selectedTo = new Date();
        $scope.selectedFrom = new Date($scope.selectedTo.getTime() - 24*3600*1000)

        $scope.getExchanges = function(){
            CRUDService.get("exchanges", null, function(result){
                $scope.exchanges = result.data;
                $scope.exchangeNames = Object.keys(result.data);
                $scope.selectedExchangeName = $scope.exchangeNames[0];
                $scope.selectedExchange = $scope.exchanges[$scope.selectedExchangeName][0];
            });
        }

        $scope.getExchanges();

        $scope.$watch("selectedExchangeName", function(newValue) {
            if(angular.isDefined(newValue) && angular.isDefined($scope.exchanges[newValue]))
                $scope.selectedExchange = $scope.exchanges[newValue][0];
            drawChart();
        });

        $scope.$watch("selectedExchange", drawChart);

        $scope.$watch("selectedChartType", drawChart);

        $scope.$watch("selectedDuration", drawChart);

        $scope.$watch("selectedTo", drawChart);

        $scope.$watch("selectedFrom", drawChart);

        function drawChart() {
            var canDraw = angular.isDefined($scope.selectedChartType) && angular.isDefined($scope.selectedExchange) &&
                angular.isDefined($scope.selectedExchange.collection) &&
                !(($scope.selectedChartType != "chart") && !angular.isDefined($scope.selectedDuration))
            if (canDraw) {
                var urlChartType = ($scope.selectedChartType == "chart") ? "" : "/" + $scope.selectedChartType;
                var url = "deals/" + $scope.selectedExchange.collection + urlChartType;
                var params = {};
                if (angular.isDefined($scope.selectedDuration))
                    params.duration = $scope.selectedDuration.value;
                params.from = $scope.selectedFrom.getTime();
                params.to = $scope.selectedTo.getTime();
                CRUDService.get(url, params, function(result) {
                    $scope.points = result.data;
                    $scope.drawingMethod = $scope.selectedChartType;
                });
            }
        }
    }];

    mainModule.directive('exchanges', function(){
        return {
            restrict: 'E',
            templateUrl: '/assets/templates/exchanges.html',
            controller: tableController
        };
    });

})()