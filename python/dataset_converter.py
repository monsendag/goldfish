# converts article and user name to article id and user id
import csv

with open('cbdata-feedback.csv', 'rb') as f:
	reader = csv.reader(f)
	next(reader, None) # skip header
	out_file = open('cbdata-feedback-anon.csv', 'wb')
	writer = csv.writer(out_file, delimiter=',')

	user_id = 0
	item_id = 0

	user_dict = {}
	item_dict = {}

	# write header
	writer.writerow(['# user_id','url_id', 'rating', 'timeOnePage','timeMovingMouse','timeSpentVerticalScrolling'])

	for row in reader:
		# check if user has an id
		if not row[0] in user_dict:
			user_id = user_id + 1
			user_dict[row[0]] = user_id
		# check if item has an id
		if not row[1] in item_dict:
			item_id = item_id + 1
			item_dict[row[1]] = item_id
		
		# replace username and item with ids instead
		row[0] = user_dict[row[0]]
		row[1] = item_dict[row[1]]

		# check for empty strings
		for i in range(2,len(row)):
			if row[i] == '':
				row[i] = 0

		writer.writerow(row)

f.close()
out_file.close()



