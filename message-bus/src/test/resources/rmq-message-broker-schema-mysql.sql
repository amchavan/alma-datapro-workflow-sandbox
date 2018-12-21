
create database if not exists draws;

use draws;
create table if not exists envelope (
	  id int(11) auto_increment,
    envelope_id varchar(64),
    consumed_timestamp varchar(64),
    expired_timestamp varchar(64),
    message varchar(2048),
    message_class varchar(256),
    origin_ip varchar(64),
    queue_name varchar(256),
    received_timestamp varchar(64),
    sent_timestamp varchar(64),
    rejected_timestamp varchar(64),
    state varchar(64),
    time_to_live int(11),
    primary key(id)
);
