require 'rubygems'
require 'neography'
require 'highline'
require 'pry'


# SETTINGS
nodes_number = 10
states_from = 100
states_to = 150


#METHODS
def random_number(top)
	1 + rand(top)
end

def random_string(letters)
	(0...letters).map { ('a'..'z').to_a[rand(26)] }.join
end

def percentage(number, tot)
	((number.to_f / tot.to_f) * 100.0).floor
end

def time_rand from = 0.0, to = Time.now
  Time.at(from + rand * (to.to_f - from.to_f))
end


#MAIN

@neo = Neography::Rest.new("http://neo4j:password@localhost:7474")

nodes = @neo.execute_query("MATCH (n) RETURN COUNT(n)")
nodes = nodes["data"][0][0]

if nodes > 0 
	reset = HighLine.new.ask "Do you wish to remove all the #{nodes} nodes in the db? (Y/n)"
	if reset == "Y"
		puts "Deleting all nodes in the db..."
		@neo.execute_query("MATCH (n) DETACH DELETE n")
	end
end

puts "Start inserting #{nodes_number} entities with #{states_from} to #{states_to} states each"
previous = 0
nodes_number.times do |i|
	#Log some status informations
	if percentage(i, nodes_number) % 5 == 0 && percentage(i, nodes_number) != previous
		puts percentage(i, nodes_number).to_s + "%"
		previous = percentage(i, nodes_number)
	end

	#Create the entity node
	node = @neo.create_node("age" => 17 + random_number(10), "name" => random_string(10))
	@neo.add_label(node, "Entity")

	#Create all the states associated to the node
	states_number = states_from + rand(states_to - states_from)
	prev_state = nil
	prev_date = nil
	states_number.times do
		state = @neo.create_node("name" => random_string(5))
		@neo.add_label(state, "State")
		rel_has_state = @neo.create_relationship("HAS_STATE", node, state)

		if prev_state != nil
			rel = @neo.create_relationship("NEXT", prev_state, state)
			date = time_rand prev_date, Time.local(9999, 12 ,31)
		else 
			rel = @neo.create_relationship("CURRENT", node, state)
			date = time_rand
		end
		
		@neo.set_relationship_properties(rel, {"date" => date})
		@neo.set_relationship_properties(rel_has_state, {"date" => date})
		prev_state = state
		prev_date = date
	end
end
