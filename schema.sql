drop database if exists pb;
create database pb;
use pb;

create table map (
	no            int primary key not null,
	asteroids     int not null,
	mass_pattern  enum ('same','123x') not null,
	orbit_pattern enum ('1/orbit','2/orbit','3-bands') not null);

create table game (
	no            int primary key not null,
	player        enum ('g1','g2','g3','g4','g6','g7','g8','g9') not null,
	map           int not null,
	max_days      int not null,
	days          int not null,
	max_mass      double not null,
	cpu_time      double not null,
	cpu_timeout   enum ('yes','no') not null,
	planet_built  enum ('yes','no') not null,
	foreign key(map) references map(no));

create table push (
	no            int not null,
	game          int not null,
	year          int not null,
	day           int not null,
	energy        double not null,
	direction     double not null,
	position_x    double not null,
	position_y    double not null,
	primary key(game, no),
	foreign key(game) references game(no));

insert into map values
( 1, 20,'same','1/orbit'),
( 2, 50,'same','1/orbit'),
( 3,100,'same','1/orbit'),
( 4, 20,'123x','1/orbit'),
( 5, 50,'123x','1/orbit'),
( 6,100,'123x','1/orbit'),
( 7, 20,'same','2/orbit'),
( 8, 50,'same','2/orbit'),
( 9,100,'same','2/orbit'),
(10, 20,'123x','2/orbit'),
(11, 50,'123x','2/orbit'),
(12,100,'123x','2/orbit'),
(13, 20,'same','3-bands'),
(14, 50,'same','3-bands'),
(15,100,'same','3-bands'),
(16, 20,'123x','3-bands'),
(17, 50,'123x','3-bands'),
(18,100,'123x','3-bands');
