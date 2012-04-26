function normalize(path) {
	var segments = path.split('/');
	var skip = 0;

	for (var i = segments.length; i >= 0; i--) {
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
function getParentId(pathStack) {
	return pathStack.length > 0 ? pathStack[pathStack.length-1] : "";
};
function expand(path, pathStack, config) {
	var isRelative = path.search(/^\./) === -1 ? false : true;
	if (isRelative) {
	    var pkg;
        if ((pkg = config.pkgs[getParentId(pathStack)])) {
            path = pkg.name + "/" + path;
        } else {
            path = getParentId(pathStack) + "/../" + path;
        }
		path = normalize(path);
	}
	for (pkgName in config.pkgs) {
	    if (path === pkgName) {
	    	return config.pkgs[pkgName].name + '/' + config.pkgs[pkgName].main;
	    }
	}
	return path;
};
