drop table envelope if exists;
drop table recipient_group if exists;

create table envelope (
	id bigint IDENTITY PRIMARY KEY,
    envelope_id varchar(64),
    consumed_timestamp varchar(64),
    expired_timestamp varchar(64),
    message varchar(1024),
    message_class varchar(64),
    origin_ip varchar(64),
    queue_name varchar(64),
    received_timestamp varchar(64),
    sent_timestamp varchar(64),
    rejected_timestamp varchar(64),
    state varchar(64),
    time_to_live BIGINT
);

create table recipient_group (
	id bigint IDENTITY PRIMARY KEY,
    group_name varchar(256),
	group_members varchar( 2048 )
);
