function _normalize(path) {
	var segments = path.split('/');
	var skip = 0;

	for (var i = (segments.length-1); i >= 0; i--) {
		var segment = segments[i];
		if (segment === '.') {
			segments.splice(i, 1);
		} else if (segment === '..') {
			segments.splice(i, 1);
			skip++;
		} else if (skip) {
			segments.splice(i, 1);
			skip--;
		}
	}
	return segments.join('/');
};

var zazlConfig;

var config = function(cfg) {
	zazlConfig = cfg;
};

var go = function(dependencies, callback) {
	var cfg = zazlConfig ? zazlConfig : {};
	cfg.baseUrl = _normalize(window.location.pathname.substring(0, window.location.pathname.lastIndexOf('/')) + "/./");
	cfg.directInject = true;
	cfg.injectUrl = "/amdjs-tests/_javascript";
	zazl(cfg, dependencies, callback);
};

var implemented = {
	basic: true,
	anon: true,
	funcString: true,
	namedWrapped: true,
	require: true,
	plugins: true
};
require = undefined;
