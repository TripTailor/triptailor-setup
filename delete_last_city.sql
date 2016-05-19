delete from hostel_service where hostel_id in (select id from hostel where location_id in (select id from location order by id desc limit 1));
delete from review where hostel_id in (select id from hostel where location_id in (select id from location order by id desc limit 1));
delete from hostel_attribute where hostel_id in (select id from hostel where location_id in (select id from location order by id desc limit 1));
delete from hostel where location_id in (select id from location order by id desc limit 1);
delete from location where id=(select id from location order by id desc limit 1);
