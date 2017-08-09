'use strict';

function load_jobs(){
	$.ajax({
		type: 'GET',
		url: current_env.get('api_url') + '/jobs',
		contentType: 'application/json',
		success: function(r) {
			if(r.error != null){
				$.growl.warning({ title: "An error occured!", message: r.error, size: 'large' });
				return;
			}

			$('#jobs-list tbody').empty();

			var c = 0;
			$.each(r.result, function(k, v){
				$('#jobs-list tbody').append(
					'<tr>'+
					'<td><a href="#module=jobs&action=get&id='+v.id+'">'+v.id+'</a></td>'+
					'<td>'+v.name+'</td>'+
					'<td>'+v.status+'</td>'+
					'</tr>'
				);

				c += 1;
			});

			if(c == 0){
				$.growl.warning({ title: "Found 0", message: "No jobs saved - you should run one!", size: 'large' });
			}
		},
	});
}

$('#jobs-create-form').on('submit', function( event ) {
	event.preventDefault();

	$.ajax({
		type: 'POST',
		url: current_env.get('api_url') + '/jobs',
		processData: false,
		data: $("#jobs-create-form").serialize(),
		success: function(r) {
			if(r.error != null){
				$.growl.warning({ title: "An error occured!", message: r.error, size: 'large' });
				return;
			}

			// if there is no error we can ignore the result and reload the list
			load_jobs();
		},
	});

	event.preventDefault();
});

$('#jobs-refresh-btn').on('click', function( event ) {
	load_jobs();
});



load_jobs();
