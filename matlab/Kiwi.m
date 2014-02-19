classdef Kiwi

    properties (GetAccess='private',SetAccess='private')
        weights = [2 1]
    end
    properties
        sparse_tensor
        tucker_tensor
        dense_tensor
    end
    
    methods (Access='private')
        function sparse_tensor = generate_sparse_tensor(obj)
            tic
            
            % read dataset from file
            M = csvread('../datasets/movielens-synthesized/ratings-synthesized.csv');
            
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
        
        function sparse_tensor = generate_random_sparse_tensor(obj)
            sparse_tensor = sptenrand([3 3 2],10); %<-- Create random testtensor.
        end
        
    end

    methods
        % constructor
        function self=Kiwi()
            
            self.sparse_tensor = self.generate_random_sparse_tensor();
          % disp(self.sparse_tensor);
            
            self.tucker_tensor = tucker_als(self.sparse_tensor, 2);
            
            
            self.dense_tensor = full(self.tucker_tensor);
           % disp(self.dense_tensor);
            
            
        end

        function tucker_tensor = tensor_factorization(obj)
            
            tucker_tensor = 1;
        end
            
        %public void refresh(Collection<Refreshable> alreadyRefreshed) {
        function refresh()
            x = 0;
        end

        %public List<RecommendedItem> recommend(long userID, int howMany,
        function items = recommend(obj, userID, howMany)
            
            recommendations = zeros(2, howMany);
          %  disp(recommendations);
            
            % retrieve the rating for all items for user with userID
            % the ratings retrieved is a column, in order to make it row it
            % is transposed 
            user_item_row = double(obj.dense_tensor(:,userID,1))';
          %  disp(user_item_row);
            
            % sort the user_item_row in order to find the items with 
            % highest values and recommend those
            % sorted_user_item_row: user_item_row sorted in descending
            % order
            % sorted_index: the index of the sorted values in the original
            % matrix
            [sorted_user_item_row, sorted_index] = sort(user_item_row,2,'descend');
          %  disp(sorted_user_item_row);
          %  disp(sorted_index);
            
            counter = 1;
            for i = 1:length(sorted_user_item_row)
                if obj.sparse_tensor(sorted_index(i),userID,1)
                    % the user have read the article so don't recommend it
                    continue
                else
                    % add recommendations to the recommendation list
                    recommendations(1,counter) = sorted_index(i);
                    recommendations(2,counter) = sorted_user_item_row(i);
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
        function x = estimatePreference(obj, userID, itemID)
            x = 0;
        end

        %public void setPreference(long userID, long itemID, float value)
        function x = setPreference(obj, userID, itemID, value)
            x = 0;
        end

        %public void removePreference(long userID, long itemID)
        function x = removePreference(obj, userID, itemID)
            x = 0;
        end

        %public DataModel getDataModel() {
        function x = getDataModel() 
            x = 0;
        end
    end
end
