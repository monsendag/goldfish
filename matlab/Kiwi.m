classdef Kiwi

    properties (GetAccess='private',SetAccess='private')
        weights = [2 1]
    end
    properties
        sparse_tensor
        tucker_tensor
        dense_tensor
        kruskal_tensor
    end
    
    methods (Access='private')
        function sparse_tensor = generate_sparse_tensor(self, dataset)
            tic
            
            % read dataset from file
            %M = csvread('../datasets/movielens-synthesized/ratings-synthesized.csv');
            M = csvread(dataset);
            
            %X = sptensor;

            % M: user id, movie id, rating, read-rating 

            numrows = size(M,1);

            rating_subs = [M(:,1:2) ones(numrows,1)];
            read_rating_subs = [M(:,1:2) 2*ones(numrows,1)];

            subs = vertcat(rating_subs, read_rating_subs);

            vals = vertcat(M(:,3), M(:,4));

            sparse_tensor = sptensor(subs,vals);
            
            
            toc
        end
        
        function sparse_tensor = generate_random_sparse_tensor(self)
            sparse_tensor = sptenrand([3 3 2],10); %<-- Create random testtensor.
        end
        
        function print_array(self, text, array)
            fprintf('%s', text);
            fprintf('%7.4f ',array);
            fprintf('\n');
        end
    end

    methods
        % constructor
        function self = Kiwi(dataset)
            
            self.sparse_tensor = self.generate_sparse_tensor(dataset);
          %  self.sparse_tensor = self.generate_random_sparse_tensor();
            
            self.tucker_tensor = tucker_als(self.sparse_tensor, 2);
          %  self.kruskal_tensor = parafac_als(self.sparse_tensor,2);
            
            self.dense_tensor = full(self.tucker_tensor);
          %  self.dense_tensor = full(self.kruskal_tensor);
            
            
        end
            
        %public void refresh(Collection<Refreshable> alreadyRefreshed) {
        function refresh()
            x = 'FUNCTION: refresh() is used';
        end

        %public List<RecommendedItem> recommend(long userID, int howMany,
        function items = recommend(self, userID, howMany)
            
            recommendations = zeros(2, howMany);
          %  disp(recommendations);
            
            % retrieve all explicit and implicit ratings for all items for 
            % user with userID 
            % the ratings retrieved is a column, in order to make it 
            % row it is transposed 
            explicit_rating_row = double(self.dense_tensor(userID,:,1))';
            implicit_rating_row = double(self.dense_tensor(userID,:,2))';
          %  self.print_array('Explicit ratings: ', explicit_rating_row);
          %  self.print_array('Implicit ratings: ', implicit_rating_row);
          
            % Ratings are combined with the formula: 2 * explicit ratings +
            % implicit ratings
            combined_ratings = self.weights(1)*explicit_rating_row ...
                + self.weights(2)*implicit_rating_row;
          %  self.print_array('Combined ratings: ', combined_ratings);
            
            % sort the combined ratings in order to find the items with 
            % highest values and recommend those
            % sorted_combined_ratings: combined_ratings sorted in descending
            % order
            % sorted_index: the index of the sorted values in the original
            % array
            [sorted_combined_ratings, sorted_index] = sort(combined_ratings,2,'descend');
          %  disp(sorted_combined_ratings);
          %  disp(sorted_index);
            
            counter = 1;
            for i = 1:length(sorted_combined_ratings)
                if self.sparse_tensor(sorted_index(i),userID,1)
                    % the user have read the article so don't recommend it
                    continue
                else
                    % add recommendations to the recommendation list
                    recommendations(1,counter) = sorted_index(i);
                    recommendations(2,counter) = sorted_combined_ratings(i);
                    counter = counter + 1;
                    
                    % check if we have enough recommendations
                    if counter > howMany
                        break;
                    end
                end
            end
            
            items = recommendations;
            
        end

        %public float estimatePreference(long userID, long itemID)
        function x = estimatePreference(self, userID, itemID)
            x = 'FUNCTION: estimatePrefence() is used';
        end

        %public void setPreference(long userID, long itemID, float value)
        function x = setPreference(self, userID, itemID, value)
            x = 'FUNCTION: setPrefence() is used';
        end

        %public void removePreference(long userID, long itemID)
        function x = removePreference(self, userID, itemID)
            x = 'FUNCTION: removePreference() is used';
        end

        %public DataModel getDataModel() {
        function x = getDataModel() 
            x = 'FUNCTION: getDataModel() is used';
        end
    end
end
