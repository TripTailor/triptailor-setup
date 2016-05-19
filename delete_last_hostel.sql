select name from hostel order by id desc limit 1;
delete from hostel_service where hostel_id=(select id from hostel order by id desc limit 1);
delete from review where hostel_id=(select id from hostel order by id desc limit 1);
delete from hostel_attribute where hostel_id=(select id from hostel order by id desc limit 1);
delete from hostel where id=(select id from hostel order by id desc limit 1);
