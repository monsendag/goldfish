classdef Kiwi

    properties (GetAccess='private',SetAccess='private')
        left
        right
    end
    properties
        color
        val
    end

    methods
        % constructor
        function self=Kiwi()
            tic
            M = csvread('../datasets/movielens-synthesized/ratings-synthesized.csv');
            %X = sptensor;

            % M: user id, movie id, rating, read-rating 

            numrows = size(M,1);

            rating_subs = [M(:,1:2) ones(numrows,1)];
            read_rating_subs = [M(:,1:2) 2*ones(numrows,1)];

            subs = vertcat(rating_subs, read_rating_subs);

            vals = vertcat(M(:,3), M(:,4));

            X = sptensor(subs,vals);
            toc
            %for i=1:size(M,1);
             %   X(M(i,1),M(i,2),1) = M(i,3);
             %   X(M(i,1),M(i,2),2) = M(i,4);
            %end
            %toc
        end

        %public void refresh(Collection<Refreshable> alreadyRefreshed) {
        function refresh()
            x = 0;
        end

        %public List<RecommendedItem> recommend(long userID, int howMany,
        function items = recommend(obj, userID, howMany)
            items = [0, 1, 2, 3];
            
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
