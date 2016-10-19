(function(Y) {
    var E = Y.util.Event,
        Dom = Y.util.Dom,
        Anim = Y.util.Anim;

        var pageInit = function() {
            var content_metas = Dom.getElementsByClassName("blojsom-accordion-content-meta"),
                response_metas = Dom.getElementsByClassName("response_meta");

            E.on(response_metas, 'click', function(e) {
                E.stopPropagation(e);
            });
            
            E.on(content_metas, 'click', function(e) {
                var target = e.target,
                    sibling = Dom.getNextSibling(target),
                    accordion_open = Dom.hasClass(target, 'blojsom-accordion-open'),
                    open, close;
                if(!accordion_open) {
                    Dom.removeClass(target, 'blojsom-accordion-closed');
                    Dom.addClass(target, 'blojsom-accordion-open');
                    open = new Anim(sibling, {height:{to:sibling.scrollHeight}}, .1);
                    open.animate();
                } else { 
                    Dom.removeClass(target, 'blojsom-accordion-open');
                    Dom.addClass(target, 'blojsom-accordion-closed'); 
                    close = new Anim(sibling, {height:{to:0}}, .1);
                    close.animate();
                }
            });
            
        };  
        E.onDOMReady(pageInit);
})(YAHOO);