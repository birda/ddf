
var SearchControlView = Backbone.View.extend({
    el: $('#searchControls'),
    events: {
        'click .back': 'back',
        'click .forward': 'forward'
    },
    views: {
        'queryForm': 'queryForm',
        'resultList': 'resultList',
        'metacardDetail': 'metacardDetail',
        'map': 'placeholder' //TODO this should go away when we do something else with the map
    },
    initialize: function() {
        _.bindAll(this, "render", "showQuery", "showResults", "showMetacardDetail", "back", "forward");
        this.selectedView = "queryForm";
        this.views.queryForm = new QueryFormView({searchControlView: this, el: this.$el.children('#searchPages')});
        this.views.resultList = new MetacardListView({searchControlView: this, el: this.$el.children('#searchPages')});
        this.views.map = mapView;
    },
    render: function() {
        this.views[this.selectedView].render();
        return this;
    },
    back: function() {
//        if(this.selectedView === "queryForm")
//        {
//            //we don't have anything behind this page yet
//        }
        if(this.selectedView === "resultList")
        {
            //go back to query
            this.showQuery();
        }
        else if(this.selectedView === "metacardDetail")
        {
            this.showResults();
        }
    },
    forward: function() {
        if(this.selectedView === "queryForm")
        {
            this.showResults();
        }
//        else if(this.selectedView === "resultList")
//        {
//            //no forward here
//        }
//        else if(this.selectedView === "metacardDetail")
//        {
//            //no forward here
//        }
    },
    showQuery: function() {
        $(".back").hide();
        $(".forward").show();
        $(".forwardNavText").text("Results ("+this.views.resultList.model.get("hits")+")");
        this.selectedView = "queryForm";
        this.render();
    },
    showResults: function(results) {
        $(".forward").hide();
        $(".back").show();
        $(".backNavText").text("Query");
        if(results) {
            this.views.map.createResultsOnMap(results);
            this.views.resultList = new MetacardListView({ results: results, mapView: this.mapView, searchControlView: this, el: this.$el.children('#searchPages') });
        }
        this.selectedView = "resultList";
        this.render();
    },
    showMetacardDetail: function(metacard) { //just guessing at what this method sig might be
        $(".back").show();
        $(".forward").hide();
        $(".backNavText").text("Results");
    }
});
