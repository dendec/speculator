(function(){
    var margin = {top: 20, right: 50, bottom: 20, left: 50};
    var chartElement
    var unwatch

    function prepareChart(element) {
        if (angular.isDefined(chartElement)) {
            chartElement.remove();
        };
        chartElement = d3.select(element).append("div").attr("class", "chart");
    }

    function drawChart(data) {
        if (angular.isDefined(data[0])) {
            var chart = fc.chart.cartesian(
                    fc.scale.dateTime(),
                    d3.scale.linear())
                .margin(margin)
                .xDomain(fc.util.extent().fields(['date'])(data))
                .yDomain(fc.util.extent().fields(['price'])(data)).yOrient('left');

            var priceLine = fc.series.line()
                .xValue(function(d) {
                    return new Date(d.date);
                })
                .yValue(function(d) {
                    return d.price;
                });

            var gridlines = fc.annotation.gridline();

            // combine using a multi-series
            var multi = fc.series.multi()
                .series([gridlines, priceLine]);

            chart.plotArea(multi);

            // render
            chartElement.datum(data).call(chart);
        }
    }

    function drawCandlestick(data) {
        if (angular.isDefined(data[0])) {
            var chart = fc.chart.cartesian(
                    fc.scale.dateTime(),
                    d3.scale.linear())
                .margin(margin)
                .xDomain(fc.util.extent().fields(['date'])(data))
                .yDomain(fc.util.extent().fields(['high', 'low'])(data)).yOrient('left');

            var candlestick = fc.series.candlestick()
                .xValue(function(d) {
                    return new Date(d.date);
                });
            var gridlines = fc.annotation.gridline();

            // combine using a multi-series
            var multi = fc.series.multi()
                .series([gridlines, candlestick]);
            chart.plotArea(multi);
            chartElement.datum(data).call(chart);
        }
    }

    function drawSma(data) {
        if (angular.isDefined(data[0])) {
            var chart = fc.chart.cartesian(
                    fc.scale.dateTime(),
                    d3.scale.linear())
                .margin(margin)
                .xDomain(fc.util.extent().fields(['date'])(data))
                .yDomain(fc.util.extent().fields(['ma7', 'ma15'])(data)).yOrient('left');

            var ma7 = fc.series.line()
                .xValue(function(d) {
                    return new Date(d.date);
                })
                .yValue(function(d) {
                    return d.ma7;
                });
            var ma15 = fc.series.line()
                .xValue(function(d) {
                    return new Date(d.date);
                })
                .yValue(function(d) {
                    return d.ma15;
                });
            var ma30 = fc.series.line()
                .xValue(function(d) {
                    return new Date(d.date);
                })
                .yValue(function(d) {
                    return d.ma30;
                });

            var gridlines = fc.annotation.gridline();

            var multi = fc.series.multi()
                .series([gridlines, ma7, ma15, ma30]);
            chart.plotArea(multi);
            chartElement.datum(data).call(chart);
        }
    }

    function controller($scope, $window) {
        //$scope.width = angular.isDefined($scope.width) ? $scope.width : $window.innerWidth - 2 * margin.left - 2 * margin.right;
        //$scope.height = angular.isDefined($scope.height) ? $scope.height : $window.innerHeight / 2 - margin.top - margin.bottom;
    };

    function link(scope, element, attrs) {
        prepareChart(element[0]);
        scope.$watch("type", function(newValue){
            console.log(newValue)
            switch (newValue) {
                case "chart":
                    if (angular.isDefined(unwatch)){
                        unwatch();
                        prepareChart(element[0]);
                    }
                    unwatch = scope.$watch("data", drawChart);
                    break;
                case "candlesticks":
                    if (angular.isDefined(unwatch)) {
                        unwatch();
                        prepareChart(element[0]);
                    }
                    unwatch = scope.$watch("data", drawCandlestick);
                    break;
                case "sma":
                    if (angular.isDefined(unwatch)) {
                        unwatch();
                        prepareChart(element[0]);
                    }
                    unwatch = scope.$watch("data", drawSma);
                    break;
            }
        });
    };

    function getDirectiveDefinition() {
        return {
            restrict: 'AE',
            scope: {
                data: '=chartData',
                type: '@' //candlesticks, chart, bars
            },
            controller: controller,
            link: link
        };
    };

    mainModule.directive('chart', getDirectiveDefinition);

})()


