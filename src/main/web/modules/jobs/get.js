'use strict';

function get_job(id){
	$.ajax({
		type: 'GET',
		url: current_env.get('api_url') + '/jobs/' + id,
		contentType: 'application/json',
		success: function(r) {
			if(r.error != null){
				$.growl.warning({ title: "An error occured!", message: r.error, size: 'large' });
			} else {
				$.each(r.result, function(k, v){
					$('#jobs-get-form input[name="'+k+'"]').val(v);
				});
			}
		},
	});
}

function get_result(id){
	$.ajax({
		type: 'GET',
		url: current_env.get('api_url') + '/jobs/' + id + '/result',
		contentType: 'application/json',
		success: function(r) {
			if(r.error != null){
				$.growl.warning({ title: "An error occured!", message: r.error, size: 'large' });
			} else {
				if(r.result == ''){
					r.result = '-- Empty --'
				}

				$('#jobs-raw-result').html(r.result);
			}
		},
	});
}

get_job(current_env.get('url_hash').get('id'));
get_result(current_env.get('url_hash').get('id'));
