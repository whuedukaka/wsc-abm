;;
;;  Food Security Project
;;  Created: Feb, 2015
;;  Contact: Peng Chen (chenpeng@indiana.edu)
;;

extensions [ table csv ]

breed [households household ]       ; Defines "households" breed
directed-link-breed [labor_exports labor_export]

;; variable definitions
globals [ debug_mode 
  road_dist_file HH_attribute_file landcover_file production_model_file gis_header_file output_world_file input_world_file output_stats_file output_labor_sharing_map output_yield_map output_yield_list
  total_number_HH num_seed_HH 
  soilType global_maize_ratio
  search_scope labor_sharing_pecent monthly_food_consum labor_land_ratio_to_weed 
  world farm_land current_day last_day current_year indexed_table gis_header sortedHHs ]
patches-own [ owned_by occupied_ratio is_seed_patch is_tentative_seed cover road_dist plant_year plant_day weed_count labor_sharing_activity yield ]
households-own [HH_internal_id HH_SIZE ADULT_EQ AREA_HA is_seed_HH eligible_importer eligible_exporter actual_importer actual_exporter hh_plant_day foodstock_amt asset_amt land_deficit cultiVar
  harvest_amt import_labor_from export_labor_to labor_exchange_foodamt was_allocated_land ]

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;  Set up functions for local testing
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
to local_launch_for_testing
  ca
  file-close-all
  
  set num_seed_HH 100
  set total_number_HH 1876
  set road_dist_file "tmp/dist_1.txt"
  set landcover_file "tmp/lc_1.txt"
  set HH_attribute_file "tmp/HH_1.txt"
  set production_model_file "InputData/subset.csv"
  set gis_header_file "tmp/header_1.txt"
  
  set output_world_file (word "tmp/ward_1_numSeedHH_" num_seed_HH "_allocation.txt")
  set output_stats_file (word "OutputData/ward_1_numSeedHH_" num_seed_HH "_stats.txt")
  set output_labor_sharing_map (word "OutputData/ward_1_numSeedHH_" num_seed_HH "_labor_sharing_map.txt")
  set output_yield_map (word "OutputData/ward_1_numSeedHH_" num_seed_HH "_yield_map.txt")
  set input_world_file output_world_file
  
  set current_day 288 ;; Oct
  set current_year 2009 ;; 2009 - 2015
  
  ;; set up parameters for labor sharing
  set search_scope 7
  set labor_sharing_pecent 0.3
  set monthly_food_consum 7
  set labor_land_ratio_to_weed 2
  set soilType "WI_VRZM080"
  set global_maize_ratio 0.7
  
end

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;  Setup procedure.  Run this to set up the environment
;;  Called by clicking the "setup" button
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
to setup
  set debug_mode false
  type "setup for " print landcover_file
  ;;reset-timer
  
  ;;clear-ticks
  set world 0
  set farm_land 0
  
  clear-turtles
  clear-patches
  clear-drawing
  clear-all-plots
  clear-output
  
  ;; set link style
  set-default-shape labor_exports "curved link"
  
  ;; set HH style
  set-default-shape households "house"
  
  ;;  Read in land cover from data file
  file-open landcover_file
  let patch-covers file-read
  file-close
  
  let total_number_patches length patch-covers
  type "total # of patches read " type landcover_file type " : " print total_number_patches
  type "total # of patches created " type landcover_file type " : " print count patches
  ( foreach sort patches patch-covers [ ask ?1 [set cover ?2 ] ] )

  set world sort patches with [cover != -9999]
  set farm_land patches with [cover = 2]
  type "total farmland for " type landcover_file type " : " print count farm_land
  
  ;; Function that displays land cover
  display_landcover
  
  ;;  Read in distance to roads from data file
  file-open road_dist_file
  let road_dists file-read
  file-close
  
  ( foreach sort patches road_dists [ ask ?1 [set road_dist ?2 ] ] )
  
  ;; import climate production data
  let index_fields (list "TRNO" "SOIL_ID..." "Pyear" "Pday" "Hyear" "Hday")
  read_index_csv production_model_file index_fields
  
  ;;  Read in land cover gis header
  file-open gis_header_file
  set gis_header (list )
  while [not file-at-end?]
  [
    set gis_header (lput file-read-line gis_header)
  ]
  file-close
  
  ;; create HHs
  let i 1    ;;  This code creates id values for each of households.  This id is used to sort households for later operations.
  create-households total_number_HH 
  [
    set is_seed_HH 0 ;; 1 stands for seed HH
    set color red
    set size 3
    set actual_importer false
    set actual_exporter false
    
    set HH_internal_id i 
    set i ( i + 1)
  ]
  
  set sortedHHs sort-on [ HH_internal_id ] households
  
  ;; Fuction that reads HH attributes from a file
  read_HH_attributes
  
  ;; Function that allocates HHs to land
  type "allocated " type allocate_HH print " households"
  
  type "finished the setup for " print landcover_file
end 

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;  Run the simulation for a year till next Nov
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
to run_for_a_year
  while [ run_biweekly != 288 ] []
  
  output_stats
  output_maps
end

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;  Run the simulation for two weeks
;;  Returns the next timestep
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
to-report run_biweekly
  type "current_day:" print current_day
  
  if ( current_day = 7 or current_day = 21 ) ;; Jan
  [
    ;; Function that decides how much foodstock left for each HH
    HH_consume_foodstock ( current_day - last_day )
    ;; Function that decides whether to sell livestock, or harvest maize, or export labor for food
    HH_deal_with_zero_foodstock
    ;; Function that exchanges labor
    HH_exchange_labor
    
    ;; Function that plants maize
    HH_plant
        
    ;; run biweekly
    set last_day current_day
    set current_day ( current_day + 14 )
    ;; return
    report current_day
  ]
  
  if ( current_day = 35 or current_day = 49 ) ;; Feb
  [
    ;; Function that decides how much foodstock left for each HH
    HH_consume_foodstock ( current_day - last_day )
    ;; Function that decides whether to sell livestock, or harvest maize, or export labor for food
    HH_deal_with_zero_foodstock
    ;; Function that exchanges labor
    HH_exchange_labor

    if ( current_day = 49 )
    [
      ;; weed once per month
      HH_weed
    ]
                
    ;; run biweekly
    set last_day current_day
    set current_day ( current_day + 14 )
    ;; return
    report current_day
  ]
  
  if ( current_day = 63 or current_day = 77 or current_day = 91 ) ;; March, note that day 91 in a leap year is in March
  [
    ;; Function that decides how much foodstock left for each HH
    HH_consume_foodstock ( current_day - last_day )
    ;; Function that decides whether to sell livestock, or harvest maize, or export labor for food
    HH_deal_with_zero_foodstock
    ;; Function that exchanges labor
    HH_exchange_labor
                
    ;; run biweekly
    set last_day current_day
    set current_day ( current_day + 14 )
    ;; return
    report current_day
  ]
  
  if ( current_day = 105 or current_day = 119 ) ;; April
  [
    ;; Function that decides how much foodstock left for each HH
    HH_consume_foodstock ( current_day - last_day )
    ;; Function that decides whether to sell livestock, or harvest maize, or export labor for food
    HH_deal_with_zero_foodstock
    ;; Function that exchanges labor
    HH_exchange_labor

    ifelse ( current_day = 105 )
    [
      set last_day current_day
      set current_day ( current_day + 14 )      
    ]
    [
      ;; weed once per month 
      HH_weed
      
      
      
      let error_count count farm_land with [ owned_by != 0 and plant_day = 0]
      if (error_count > 0)
      [
        type "ERROR:" type "there are " type error_count print " farm cells not planting"
      ]
      
      ;; Function that harvests maize
      ask households
      [
        HH_harvest HH_internal_id
      ]
      
      ;; Apirl to Nov is dry season
      set last_day current_day
      set current_day 288    
    ]
    
    report current_day  
  ]

  if ( current_day = 288 or current_day = 302 )
  [
    ;; Function that decides how much foodstock left for each HH
    HH_consume_foodstock ( current_day - last_day )
    ;; Function that decides whether to sell livestock, or harvest maize, or export labor for food
    HH_deal_with_zero_foodstock
    ;; Function that exchanges labor
    HH_exchange_labor
    
    ;; Function that plants maize
    HH_plant
    
    ;; run biweekly
    set last_day current_day
    set current_day ( current_day + 14 )
    ;; return
    report current_day
  ]
  
  if ( current_day = 316 or current_day = 330 )
  [
    ;; Function that decides how much foodstock left for each HH
    HH_consume_foodstock ( current_day - last_day )
    ;; Function that decides whether to sell livestock, or harvest maize, or export labor for food
    HH_deal_with_zero_foodstock
    ;; Function that exchanges labor
    HH_exchange_labor
    
    ;; Function that plants maize
    HH_plant
    
    ;; run biweekly
    set last_day current_day
    set current_day ( current_day + 14 )
    ;; return
    report current_day
  ]
  
  if ( current_day = 344 or current_day = 358 )
  [
    ;; Function that decides how much foodstock left for each HH
    HH_consume_foodstock ( current_day - last_day )
    ;; Function that decides whether to sell livestock, or harvest maize, or export labor for food
    HH_deal_with_zero_foodstock
    ;; Function that exchanges labor
    HH_exchange_labor

    ;; Function that plants maize
    HH_plant
    
    ;; proceed to next year
    ifelse (current_day = 358) 
    [ 
      set current_day 7
      set current_year (current_year + 1 )
    ]
    [
      ;; run biweekly
      set last_day current_day
      set current_day ( current_day + 14 )
    ]
    
    ;; return
    report current_day
  ]
  
  report -1
end

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;  HHs planting maize
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
to HH_plant
  ask households with [ current_day = hh_plant_day ]
  [
    let hh_id HH_internal_id
    ask farm_land with [ owned_by = hh_id ]
    [
      set plant_day current_day
      set plant_year current_year
      set weed_count 0     
    ]
  ] 
end

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;  HHs weeding
;;  if the land has sufficient labor
;;  or if the HH has imported labor
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
to HH_weed
  ask households
  [
    if (AREA_HA != 0) and ( ((ADULT_EQ / AREA_HA) > labor_land_ratio_to_weed) or (actual_importer = true) )
    [
      let hh_id HH_internal_id
      ask farm_land with [ owned_by = hh_id ]
      [
        set weed_count (weed_count + 1)
      ]      
    ]
  ] 
end

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;  HHs harvesting maize
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
to HH_harvest [ hh_id ]
  ;; calculate potential
  let potential_yield ( calculate_potential_yield hh_id )
  
  ;; update HHs' foodstock
  if (potential_yield > 0)
  [
    ;; add new crop to HHs' foodstock
    ask households with [ HH_internal_id = hh_id ] 
    [ 
      set harvest_amt potential_yield
      set foodstock_amt (foodstock_amt + potential_yield) 
    ]

    if ( debug_mode ) 
    [
      type "HH: " type hh_id type " gain foodstock from harvest: " print potential_yield
    ]  
          
    ;; mark farmland as harvested
    ask farm_land with [ (owned_by = hh_id) ]
    [
      set plant_year 0
      set plant_day 0
      ;; TODO clear others
    ]  
  ]
end

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;  Update the foodstock for each HH
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
to HH_consume_foodstock [ day_eplased ] ;; unit: day
  ;; update HHs' foodstock
  ask households
  [ 
    let month_elapsed (day_eplased / 30)
    let foodstock_con ( HH_SIZE * monthly_food_consum * month_elapsed)
    set foodstock_amt ( max list ( foodstock_amt - foodstock_con ) 0 )
  ]  
end

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;  HHs with zero foodstock decides whether to sell livestock, or harvest maize,
;;  or export labor for food
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
to HH_deal_with_zero_foodstock
  ask households with [ foodstock_amt = 0 ]
  [ 
    ifelse ( asset_amt > 0 )
    [
      ;; sell livestock to buy food
      set asset_amt ( asset_amt - 1 ) 
      set foodstock_amt ( foodstock_amt + 5000 );; TODO: how much foodstock gain
      
      
      if ( debug_mode ) 
      [
        type "HH: " type HH_internal_id print " sell livestock"
      ]
    ]
    [
      if ( HH_decide_to_harvest HH_internal_id )
      [
        HH_harvest HH_internal_id
      ]
      ;; nothing has been done, so the foodstock remains zero
      ;; can only export labor to gain food
    ]
  ]   
end

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;  HH decides whether to harvest at this moment
;;  TODO: could be selling asset instead
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
to-report HH_decide_to_harvest [ hh_id ]
  ifelse (any? farm_land with [ (owned_by = hh_id) and (plant_day != 0) ]) ;; and ( current_month >= 3 ) and ( current_month <= 5 )
  [
    report true
  ]
  [
    report false
  ]
end

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;  Read in household attributes
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
to read_HH_attributes
  
  file-close
  ;;  Read in attributes for each HH
  ifelse ( file-exists? HH_attribute_file )
  [
    file-open HH_attribute_file
    let header file-read-line;; reads in first header line of text file
    
    let i 1
    
    while [ not file-at-end? ]
    [
      let readline file-read-line
      let split-result read-from-string ( word "[" readline "]" )
      
      ask households with [ HH_internal_id = i ]
      [ 
        set ADULT_EQ item 0 split-result
        set cultiVar item 2 split-result
;        set AREA_HA round (item 2 split-result) ;; NOTICE: round the area to the nearest integer.
;        if ( AREA_HA = 0 ) [ set AREA_HA 1 ] ;; Special case: set 0 (original or rounded) CultArea to 1
        set hh_plant_day item 3 split-result
        set AREA_HA item 4 split-result
        set AREA_HA round (AREA_HA * global_maize_ratio)
        set HH_SIZE item 5 split-result
        
;        if (hh_plant_day <= 28)
;        [
;          set hh_plant_year (hh_plant_year + 1)
;        ]
        
        ;; TODO: some fake initial value
        set foodstock_amt 1000000
        set asset_amt 1
        
        if ( debug_mode ) 
        [
          type " ADULT_EQ " type ADULT_EQ
          type " MAIZE_AREA_HA " type AREA_HA
          type " HH_SIZE " print HH_SIZE
          type " cultiVar " print cultiVar
          type " HH_planting_day " print hh_plant_day
        ]
        
        set i i + 1
      ]
    ]
    
    file-close
  ]
  [ user-message (word "There is no File" HH_attribute_file " in inputdata directory!") ]

end

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;  allocate land to households
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
to-report allocate_HH
  let i 1
  let lackOfLand false
  
  ;; allocate up to 'num_seed_HH' seed HHs
  while [ (i <= num_seed_HH) and (not lackOfLand) ]
  [
    ;; set this HH to be a seed HH
    ask item (i - 1) sortedHHs [set is_seed_HH 1]
    
    ;; find a seed cell, then allocate one HH               
    ask n-of 1 farm_land with [ owned_by = 0 ]
    [
      allocate_many_farmland_to_HH i
    ] 
    
    set i ( i + 1 ) 
    
    ;; note that the bool var lackOfLand is set as a global indicator for judging the outside loop 
    set lackOfLand (count farm_land with [ owned_by = 0] = 0) 
    if (lackOfLand) [type landcover_file print " lack of farm land!"]
  ]
  
  ;; allocate non-seed HHs
  while [ (i <= count households) and (not lackOfLand) ]
  [
    ;; find a seed cell, then allocate one HH               
    let unoccupied_seed farm_land with [ (is_tentative_seed = 1) and (owned_by = 0) ]
    
    ifelse ( (count unoccupied_seed) != 0 )
    [
      ask n-of 1 farm_land with [ (is_tentative_seed = 1) and (owned_by = 0) ]
      [
        allocate_many_farmland_to_HH i
      ] 
    ]
    [
      ;; if there is no unoccupied seed patch, just use any unoccupied farmland
;      ask n-of 1 farm_land with [ owned_by = 0 ]
;      [
;        allocate_many_farmland_to_HH i
;      ]

      type landcover_file print " cannot find unoccupied seed farm land!"
      report i
    ]
        
    set i ( i + 1 ) 
    
    ;; note that the bool var lackOfLand is set as a global indicator for judging the outside loop 
    set lackOfLand (count farm_land with [ owned_by = 0 ] = 0) 
    if (lackOfLand) [type landcover_file print " lack of farm land!"]
  ]
  
  report i
end

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;  allocate a single patch of farmland to a HH
;;  This function is called inside the farmland patch
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
to allocate_farmland_to_HH [ hh_id allocate_area ] ;; Household id, area to allocate
  ;; set pcolor wrap-color owned_by
  set owned_by hh_id
  set pcolor wrap-color hh_id
  set occupied_ratio allocate_area
  
  ;; set its neighbors within a certain distance to be tentative seed patches
  ask farm_land in-radius 4 with [ owned_by = 0 ] [ set is_tentative_seed 1 ]
  set is_tentative_seed 0

  if ( debug_mode ) 
  [
    type "assign to " 
    show owned_by 
  ]  
end

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;  allocate farmland to a HH's need
;;  This function is called inside the farmland patch
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
to allocate_many_farmland_to_HH [ id ]  ;; Household id
  let ha_required [AREA_HA] of item (id - 1) sortedHHs
  let hh_adult_eq [ADULT_EQ] of item (id - 1) sortedHHs
  
  ;; assign the seed cell
  set is_seed_patch 1
  
  ;; give the seed cell coordinates to the HH
  let hh_x_cor pxcor
  let hh_y_cor pycor
  ask item (id - 1) sortedHHs 
  [ 
    setxy hh_x_cor hh_y_cor 
    set was_allocated_land 1  
  ]
  
  ifelse ( ha_required > 1 )
  [
    set ha_required ( ha_required - 1 )
    
    ;; allocate the seed cell to HH
    allocate_farmland_to_HH id 1
    
    ;; look around for empty farm land
    let lackOfLand false
    while [ ( ha_required > 0 ) and (not lackOfLand) ]
    [
      let x_cor pxcor
      let y_cor pycor
      let width 7
      let height 7
      
      ;; if there is no free land within 700m, then expand the searching scope, until 1500m
      while [ (width < 15) and (count farm_land with [ (owned_by = 0) and (pxcor >= (x_cor - width) ) and (pxcor <= (x_cor + width) ) and (pycor <= (y_cor + height)) and (pycor >= (y_cor - height) )] = 0 ) ]
      [
        set width ( width + 1 )
        set height ( height + 1 )
      ]
      
      ifelse ( width = 15) 
      [
        print "not enough land within 1.5km!"
        ;; this bool var is now used for local judgement
        set lackOfLand true
        
        ask item (id - 1) sortedHHs [ set land_deficit ha_required ] 
      ]
      [
        ask n-of 1 farm_land with [ (owned_by = 0) and (pxcor >= (x_cor - width) ) and (pxcor <= (x_cor + width) ) and (pycor <= (y_cor + height)) and (pycor >= (y_cor - height) )]
        [  
          ifelse ( ha_required > 1 )
          [
            allocate_farmland_to_HH id 1
            set ha_required ( ha_required - 1 )
          ]
          [
            allocate_farmland_to_HH id ha_required
            set ha_required 0
          ]            
        ]
      ]
      
    ]
  ]
  [        
    ;; allocate the seed cell to HH
    allocate_farmland_to_HH id ha_required
  ]
end

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;  Labor exchange
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
to HH_exchange_labor
  ;; clear all previous labor sharing/allocation
  clear-links
  
  ask households
  [
    set import_labor_from -1
    set export_labor_to -1
  ]   

  ;; get the number of labor importers/exporters
  let num_of_participants floor (labor_sharing_pecent * ( count households ))
  
  ;; get the top and bottom 'labor_sharing_pecent' of the households ordered by (AREA_HA / ADULT_EQ)
  let importer_candidates max-n-of num_of_participants households with [ ADULT_EQ != 0 ] [ (AREA_HA / ADULT_EQ) ]
  set importer_candidates ( importer_candidates with [ (foodstock_amt - monthly_food_consum * HH_SIZE * (288 - current_day) / 30) > 0 ] )
   
  let exporter_candidates min-n-of num_of_participants households with [ ADULT_EQ != 0 ] [ (AREA_HA / ADULT_EQ) ]
  
  ;; mark the qualification of importers and exporters
  ask importer_candidates
  [
    set eligible_importer true
  ]
  
  ask exporter_candidates
  [
    set eligible_exporter true
  ]
  
  let export_count 0
  ask exporter_candidates
  [
    ;; TODO: can we assume all HHs here have excess labor?

    ;; try to export labor
    let hh_id HH_internal_id
    let required_foodamt (monthly_food_consum * HH_SIZE)
    let x_cor xcor
    let y_cor ycor
    
    let qualified_importers importer_candidates with 
    [ (xcor >= (x_cor - search_scope)) and (xcor <= (x_cor + search_scope)) and 
      (ycor <= (y_cor + search_scope)) and (ycor >= (y_cor - search_scope)) and
      (actual_importer = false) and
      (decide_to_import_labor HH_internal_id required_foodamt) and  
      (HH_internal_id != hh_id)
    ]
    
    if (count qualified_importers) > 0
    [
      ;;;;;;;;;;;;;;;;;;;;;;;
      ;; update target hh (importer) attributes
      ;;;;;;;;;;;;;;;;;;;;;;;;;;
      let target_HH one-of qualified_importers
      ask target_HH
      [ 
        set foodstock_amt ( max list (foodstock_amt - required_foodamt) 0 )
        ;; now we remember how much food does the labor sharing cost
        ;; table:put import_adult_table hh_id required_foodamt
        set import_labor_from hh_id
        set labor_exchange_foodamt required_foodamt
        
        ;; mark the labor sharing activity
        set actual_importer true
      ]
      
      ;;;;;;;;;;;;;;;;;;;;;;;;;
      ;; update current hh (exporter) attributes
      ;;;;;;;;;;;;;;;;;;;;;;;;;
      set foodstock_amt (foodstock_amt + required_foodamt)
      ;; table:put export_adult_table [HH_internal_id] of target_HH required_foodamt
      set export_labor_to [HH_internal_id] of target_HH
      set labor_exchange_foodamt required_foodamt
      ;; mark the labor sharing activity
      set actual_exporter true
      set export_count (export_count + 1)  
      
      ;; create a shape link
      create-labor_export-to target_HH [ set color black ]
      
      if ( debug_mode ) 
      [  
        type hh_id
        type " "
        type "export to "
        type [HH_internal_id] of target_HH
        type " cost:"
        print required_foodamt
      ]
      
    ]
  ]
  
end

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;  Determine whether a HH has excess foodstock to import labor
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
to-report decide_to_import_labor [ importer_id required_foodamt ]
  ;; let HH_importer one-of households with [ (HH_internal_id = importer_id) ]
  let HH_importer item (importer_id - 1) sortedHHs
  
  ;; reserve the amt of food needed till Apirl before harvesting
  let foodstock_excess [ (foodstock_amt - monthly_food_consum * HH_SIZE * (288 - current_day) / 30) ] of HH_importer
  
  ;; if the exporter has enough excess foodstock
  ifelse (foodstock_excess >= required_foodamt)
  [
    report true  
  ]
  [
    report false
  ]
end

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;  Calculate the production for HHs that have mature maize
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
to-report calculate_potential_yield [ hh_id ]
  let total_yield 0
  let hh_cultiVar [cultiVar] of item (hh_id - 1) sortedHHs
  
  ask farm_land with [ (owned_by = hh_id) and ( plant_day != 0 ) ]
  [ 
    let key (list )
    set key (lput hh_cultiVar key)
    set key (lput soilType key)
    set key (lput plant_year key)
    set key (lput plant_day key)
    set key (lput current_year key)
    set key (lput current_day key)
    
    let yield_per_ha table:get indexed_table key
    ;; occupied_ratio could be zero for hh with zero 'CultArea'
    set yield yield_per_ha * occupied_ratio
    set total_yield ( total_yield + yield )
  ]
  
  report total_yield
end

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;  Import/Export world
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
to export_world
  type "export world for " print landcover_file
  export-world output_world_file
  type "finished exporting world for " print landcover_file
end

to import_world
  import-world input_world_file
end

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;  Display functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
to display_landcover
  ask households [ hide-turtle ]
  ask labor_exports [ hide-link ]
  
  ask patches [ set pcolor white ]
  ;;ag
  ask patches with [cover = 2] [ set pcolor 35 ]
  ;;non-ag
  ask patches with [cover = 0] [ set pcolor 53 ]
  ask patches with [cover = 1] [ set pcolor 53 ]
  ask patches with [cover = 3] [ set pcolor 53 ]
  ask patches with [cover = 4] [ set pcolor 53 ]
  ask patches with [cover = 5] [ set pcolor 53 ]
  ;;nodata
  ask patches with [cover = -9999] [ set pcolor white]
end

to display_HH
  ask patches [ set pcolor white ]
  ;; ask farm_land with [ is-seed = 1] [set pcolor wrap-color owned_by]
  ask households [ show-turtle ]
  ask labor_exports [ show-link ]
end

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Read and index CSV files
;; @param: file -- csv file; index_fields -- the fields to be used as keys 
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
to read_index_csv [ file index_fields ]
  file-close
  file-open file
  
  let delimeter ","
  let header (csv:from-row file-read-line delimeter)
  print header
  
  ;; mapping from the field name to its index in the header array
  let field_dict table:make
  let index 0
  while [index < length header]
  [
    table:put field_dict (item index header) index
    set index (index + 1)
  ]
  
  set indexed_table table:make
  while [ not file-at-end? ]
  [
    let read-line file-read-line
    let line (csv:from-row read-line delimeter)
    let yield_per_ha (item 7 line)
    
    ;; let key to be a list of keywords
    let key (list )
    ;; or we can combine the keywords into a single string
    ;; let key ""
    
    ;; build the key using all fields in 'index_fields'
    set index 0
    while [ index < length index_fields ]
    [
      set key (lput (item (table:get field_dict (item index index_fields)) line) key)
      ;; set key (word key " " (item (table:get field_dict (item index index_fields)) line))
      set index (index + 1)
    ]
    
    ;; add the indexed entry into the lookup table
    table:put indexed_table key yield_per_ha
    ;; table:put indexed_table key read-line
  ]
  
  file-close
end

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;  Output data for post-analysis
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
to output_stats
  let file output_stats_file
  file-close-all
  if file-exists? file
    [ file-delete file ]
  
  file-open file
  
  ;; print simulation parameters for records
  file-print "## simulation parameters"
  file-type "total_number_HH: " file-print total_number_HH
  file-type "num_seed_HH: " file-print num_seed_HH
  file-type "current_year: " file-print current_year
  file-type "soilType: " file-print soilType
  file-type "search_scope (maximal labor sharing scope to each direction, 100m): " file-print search_scope
  file-type "labor_sharing_pecent (top/bottom percent of household to exchange labor): " file-print labor_sharing_pecent
  file-type "monthly_food_consum (food consumption per person, kg): " file-print monthly_food_consum
  file-type "labor_land_ratio_to_weed (minimal adult equivalent per ha land to be able to weed): " file-print labor_land_ratio_to_weed
  
  file-print "## production summary"
  let total_yield sum [yield] of farm_land
  let avg_yield (total_yield / count farm_land with [ owned_by != 0 ])
  
  file-type "total yield (kg): " file-print total_yield
  file-type "avg yield (kg/ha): " file-print avg_yield

  ;; print output file paths and names
  file-print "## outputs"
  file-type "report file: " file-print output_stats_file
  file-type "yield map file: " file-print output_yield_map
  file-type "labor sharing map file: " file-print output_labor_sharing_map
  
  ;; print stats for analysis
  file-print "## labor exchange stats"
  let numOfActualExporter (count households with [actual_exporter = true])
  let numOfActualImporter (count households with [actual_Importer = true])
  
  file-type "number of HHs that want labor but did not find labor:" file-print ((count households with [eligible_importer = true]) - numOfActualImporter)  
  file-type "number of HHs that want labor and found labor:" file-print numOfActualImporter
  file-type "number of HHs that want to provide labor but did not provide labor:" file-print ((count households with [eligible_exporter = true]) - numOfActualExporter)
  file-type "number of HHs that want to provide labor and provided labor:" file-print numOfActualExporter
  file-type "number of HHs that has OP0001:" file-print (count households with [cultiVar = 1])
  file-type "number of HHs that uses HY0001:" file-print (count households with [cultiVar = 3])
  
  ;; print stats for analysis
  file-print "## land allocation stats"
  file-type "total area of Ag land: " file-print count farm_land
  file-type "area of Ag land that is not allocated: " file-print count farm_land with [ owned_by = 0 ]
  file-type "num of HHs that cannot find unoccupied land nearby: " file-print count households with [ land_deficit != 0 ]
  file-type "num of HHs that are not allocated any land: " file-print count households with [ was_allocated_land = 0 ]
  
  let deficit (sum [ AREA_HA ] of households - (count farm_land with [ owned_by != 0 ]))
  if deficit < 0 [ set deficit 0 ]
  file-type "land deficit: " file-print deficit  
  
  file-flush
  file-close
end

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;  Output two maps (txt file)
;;  1. a map of crop yields for each cell
;;  2. The labor sharing status for each cell (importer, exporter, or neither);
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
to output_maps  
  ;; prepare to export labor sharing map
  ask households
  [
    let hh_id HH_internal_id
    let labor_sharing_value 0
    if (actual_exporter = true)
    [
      set labor_sharing_value -1
    ]
    
    if (actual_Importer = true)
    [
      set labor_sharing_value 1
    ]
    
    ask farm_land with [ owned_by = hh_id ]
    [
      set labor_sharing_activity labor_sharing_value
    ]
  ]

  let file output_labor_sharing_map
  file-close-all
  if file-exists? file
    [ file-delete file ]
  file-open file

  ;; insert the header with geo-ref information
  foreach gis_header
  [
    file-print ?
  ]
  
  ;; print patch by patch
  foreach sort patches
  [
    file-type (word [labor_sharing_activity] of ?1 " ")
  ]
  
  file-flush
  file-close 
  
  set file output_yield_map
  if file-exists? file
    [ file-delete file ]
  file-open file

  ;; insert the header with geo-ref information  
  foreach gis_header
  [
    file-print ?
  ]
    
  foreach sort patches
  [
    file-type (word [yield] of ?1 " ")
  ]

  file-flush
  file-close

end

to output_yield  
  let file output_yield_list
  if file-exists? file
    [ file-delete file ]
  file-open file
    
  foreach sort farm_land with [ owned_by != 0 ]
  [
    file-type (word [yield] of ?1 "\n")
  ]

  file-flush
  file-close

end
@#$#@#$#@
GRAPHICS-WINDOW
174
13
564
944
-1
-1
2.0
1
10
1
1
1
0
1
1
1
0
189
0
449
0
0
1
ticks
30.0

BUTTON
11
64
83
97
setup
setup
NIL
1
T
OBSERVER
NIL
NIL
NIL
NIL
1

BUTTON
12
296
117
329
export_world
export_world
NIL
1
T
OBSERVER
NIL
NIL
NIL
NIL
1

BUTTON
9
26
161
59
launch_for_testing
local_launch_for_testing
NIL
1
T
OBSERVER
NIL
NIL
NIL
NIL
1

BUTTON
12
147
85
182
run
type \"next day: \" print run_biweekly
NIL
1
T
OBSERVER
NIL
NIL
NIL
NIL
1

BUTTON
14
241
128
274
repeatly_run
type \"next day: \" print run_biweekly
T
1
T
OBSERVER
NIL
NIL
NIL
NIL
1

BUTTON
14
194
147
227
run_for_a_year
run_for_a_year
NIL
1
T
OBSERVER
NIL
NIL
NIL
NIL
1

@#$#@#$#@
## WHAT IS IT?

This section could give a general understanding of what the model is trying to show or explain.

## HOW IT WORKS

This section could explain what rules the agents use to create the overall behavior of the model.

## HOW TO USE IT

This section could explain how to use the model, including a description of each of the items in the interface tab.

## THINGS TO NOTICE

This section could give some ideas of things for the user to notice while running the model.

## THINGS TO TRY

This section could give some ideas of things for the user to try to do (move sliders, switches, etc.) with the model.

## EXTENDING THE MODEL

This section could give some ideas of things to add or change in the procedures tab to make the model more complicated, detailed, accurate, etc.

## NETLOGO FEATURES

This section could point out any especially interesting or unusual features of NetLogo that the model makes use of, particularly in the Procedures tab.  It might also point out places where workarounds were needed because of missing features.

## RELATED MODELS

This section could give the names of models in the NetLogo Models Library or elsewhere which are of related interest.

## CREDITS AND REFERENCES

This section could contain a reference to the model's URL on the web if it has one, as well as any other necessary credits or references.
@#$#@#$#@
default
true
0
Polygon -7500403 true true 150 5 40 250 150 205 260 250

airplane
true
0
Polygon -7500403 true true 150 0 135 15 120 60 120 105 15 165 15 195 120 180 135 240 105 270 120 285 150 270 180 285 210 270 165 240 180 180 285 195 285 165 180 105 180 60 165 15

arrow
true
0
Polygon -7500403 true true 150 0 0 150 105 150 105 293 195 293 195 150 300 150

box
false
0
Polygon -7500403 true true 150 285 285 225 285 75 150 135
Polygon -7500403 true true 150 135 15 75 150 15 285 75
Polygon -7500403 true true 15 75 15 225 150 285 150 135
Line -16777216 false 150 285 150 135
Line -16777216 false 150 135 15 75
Line -16777216 false 150 135 285 75

bug
true
0
Circle -7500403 true true 96 182 108
Circle -7500403 true true 110 127 80
Circle -7500403 true true 110 75 80
Line -7500403 true 150 100 80 30
Line -7500403 true 150 100 220 30

butterfly
true
0
Polygon -7500403 true true 150 165 209 199 225 225 225 255 195 270 165 255 150 240
Polygon -7500403 true true 150 165 89 198 75 225 75 255 105 270 135 255 150 240
Polygon -7500403 true true 139 148 100 105 55 90 25 90 10 105 10 135 25 180 40 195 85 194 139 163
Polygon -7500403 true true 162 150 200 105 245 90 275 90 290 105 290 135 275 180 260 195 215 195 162 165
Polygon -16777216 true false 150 255 135 225 120 150 135 120 150 105 165 120 180 150 165 225
Circle -16777216 true false 135 90 30
Line -16777216 false 150 105 195 60
Line -16777216 false 150 105 105 60

car
false
0
Polygon -7500403 true true 300 180 279 164 261 144 240 135 226 132 213 106 203 84 185 63 159 50 135 50 75 60 0 150 0 165 0 225 300 225 300 180
Circle -16777216 true false 180 180 90
Circle -16777216 true false 30 180 90
Polygon -16777216 true false 162 80 132 78 134 135 209 135 194 105 189 96 180 89
Circle -7500403 true true 47 195 58
Circle -7500403 true true 195 195 58

circle
false
0
Circle -7500403 true true 0 0 300

circle 2
false
0
Circle -7500403 true true 0 0 300
Circle -16777216 true false 30 30 240

cow
false
0
Polygon -7500403 true true 200 193 197 249 179 249 177 196 166 187 140 189 93 191 78 179 72 211 49 209 48 181 37 149 25 120 25 89 45 72 103 84 179 75 198 76 252 64 272 81 293 103 285 121 255 121 242 118 224 167
Polygon -7500403 true true 73 210 86 251 62 249 48 208
Polygon -7500403 true true 25 114 16 195 9 204 23 213 25 200 39 123

cylinder
false
0
Circle -7500403 true true 0 0 300

dot
false
0
Circle -7500403 true true 90 90 120

face happy
false
0
Circle -7500403 true true 8 8 285
Circle -16777216 true false 60 75 60
Circle -16777216 true false 180 75 60
Polygon -16777216 true false 150 255 90 239 62 213 47 191 67 179 90 203 109 218 150 225 192 218 210 203 227 181 251 194 236 217 212 240

face neutral
false
0
Circle -7500403 true true 8 7 285
Circle -16777216 true false 60 75 60
Circle -16777216 true false 180 75 60
Rectangle -16777216 true false 60 195 240 225

face sad
false
0
Circle -7500403 true true 8 8 285
Circle -16777216 true false 60 75 60
Circle -16777216 true false 180 75 60
Polygon -16777216 true false 150 168 90 184 62 210 47 232 67 244 90 220 109 205 150 198 192 205 210 220 227 242 251 229 236 206 212 183

fish
false
0
Polygon -1 true false 44 131 21 87 15 86 0 120 15 150 0 180 13 214 20 212 45 166
Polygon -1 true false 135 195 119 235 95 218 76 210 46 204 60 165
Polygon -1 true false 75 45 83 77 71 103 86 114 166 78 135 60
Polygon -7500403 true true 30 136 151 77 226 81 280 119 292 146 292 160 287 170 270 195 195 210 151 212 30 166
Circle -16777216 true false 215 106 30

flag
false
0
Rectangle -7500403 true true 60 15 75 300
Polygon -7500403 true true 90 150 270 90 90 30
Line -7500403 true 75 135 90 135
Line -7500403 true 75 45 90 45

flower
false
0
Polygon -10899396 true false 135 120 165 165 180 210 180 240 150 300 165 300 195 240 195 195 165 135
Circle -7500403 true true 85 132 38
Circle -7500403 true true 130 147 38
Circle -7500403 true true 192 85 38
Circle -7500403 true true 85 40 38
Circle -7500403 true true 177 40 38
Circle -7500403 true true 177 132 38
Circle -7500403 true true 70 85 38
Circle -7500403 true true 130 25 38
Circle -7500403 true true 96 51 108
Circle -16777216 true false 113 68 74
Polygon -10899396 true false 189 233 219 188 249 173 279 188 234 218
Polygon -10899396 true false 180 255 150 210 105 210 75 240 135 240

house
false
0
Rectangle -7500403 true true 45 120 255 285
Rectangle -16777216 true false 120 210 180 285
Polygon -7500403 true true 15 120 150 15 285 120
Line -16777216 false 30 120 270 120

leaf
false
0
Polygon -7500403 true true 150 210 135 195 120 210 60 210 30 195 60 180 60 165 15 135 30 120 15 105 40 104 45 90 60 90 90 105 105 120 120 120 105 60 120 60 135 30 150 15 165 30 180 60 195 60 180 120 195 120 210 105 240 90 255 90 263 104 285 105 270 120 285 135 240 165 240 180 270 195 240 210 180 210 165 195
Polygon -7500403 true true 135 195 135 240 120 255 105 255 105 285 135 285 165 240 165 195

line
true
0
Line -7500403 true 150 0 150 300

line half
true
0
Line -7500403 true 150 0 150 150

pentagon
false
0
Polygon -7500403 true true 150 15 15 120 60 285 240 285 285 120

person
false
0
Circle -7500403 true true 110 5 80
Polygon -7500403 true true 105 90 120 195 90 285 105 300 135 300 150 225 165 300 195 300 210 285 180 195 195 90
Rectangle -7500403 true true 127 79 172 94
Polygon -7500403 true true 195 90 240 150 225 180 165 105
Polygon -7500403 true true 105 90 60 150 75 180 135 105

plant
false
0
Rectangle -7500403 true true 135 90 165 300
Polygon -7500403 true true 135 255 90 210 45 195 75 255 135 285
Polygon -7500403 true true 165 255 210 210 255 195 225 255 165 285
Polygon -7500403 true true 135 180 90 135 45 120 75 180 135 210
Polygon -7500403 true true 165 180 165 210 225 180 255 120 210 135
Polygon -7500403 true true 135 105 90 60 45 45 75 105 135 135
Polygon -7500403 true true 165 105 165 135 225 105 255 45 210 60
Polygon -7500403 true true 135 90 120 45 150 15 180 45 165 90

sheep
false
0
Rectangle -7500403 true true 151 225 180 285
Rectangle -7500403 true true 47 225 75 285
Rectangle -7500403 true true 15 75 210 225
Circle -7500403 true true 135 75 150
Circle -16777216 true false 165 76 116

square
false
0
Rectangle -7500403 true true 30 30 270 270

square 2
false
0
Rectangle -7500403 true true 30 30 270 270
Rectangle -16777216 true false 60 60 240 240

star
false
0
Polygon -7500403 true true 151 1 185 108 298 108 207 175 242 282 151 216 59 282 94 175 3 108 116 108

target
false
0
Circle -7500403 true true 0 0 300
Circle -16777216 true false 30 30 240
Circle -7500403 true true 60 60 180
Circle -16777216 true false 90 90 120
Circle -7500403 true true 120 120 60

tree
false
0
Circle -7500403 true true 118 3 94
Rectangle -6459832 true false 120 195 180 300
Circle -7500403 true true 65 21 108
Circle -7500403 true true 116 41 127
Circle -7500403 true true 45 90 120
Circle -7500403 true true 104 74 152

triangle
false
0
Polygon -7500403 true true 150 30 15 255 285 255

triangle 2
false
0
Polygon -7500403 true true 150 30 15 255 285 255
Polygon -16777216 true false 151 99 225 223 75 224

truck
false
0
Rectangle -7500403 true true 4 45 195 187
Polygon -7500403 true true 296 193 296 150 259 134 244 104 208 104 207 194
Rectangle -1 true false 195 60 195 105
Polygon -16777216 true false 238 112 252 141 219 141 218 112
Circle -16777216 true false 234 174 42
Rectangle -7500403 true true 181 185 214 194
Circle -16777216 true false 144 174 42
Circle -16777216 true false 24 174 42
Circle -7500403 false true 24 174 42
Circle -7500403 false true 144 174 42
Circle -7500403 false true 234 174 42

turtle
true
0
Polygon -10899396 true false 215 204 240 233 246 254 228 266 215 252 193 210
Polygon -10899396 true false 195 90 225 75 245 75 260 89 269 108 261 124 240 105 225 105 210 105
Polygon -10899396 true false 105 90 75 75 55 75 40 89 31 108 39 124 60 105 75 105 90 105
Polygon -10899396 true false 132 85 134 64 107 51 108 17 150 2 192 18 192 52 169 65 172 87
Polygon -10899396 true false 85 204 60 233 54 254 72 266 85 252 107 210
Polygon -7500403 true true 119 75 179 75 209 101 224 135 220 225 175 261 128 261 81 224 74 135 88 99

wheel
false
0
Circle -7500403 true true 3 3 294
Circle -16777216 true false 30 30 240
Line -7500403 true 150 285 150 15
Line -7500403 true 15 150 285 150
Circle -7500403 true true 120 120 60
Line -7500403 true 216 40 79 269
Line -7500403 true 40 84 269 221
Line -7500403 true 40 216 269 79
Line -7500403 true 84 40 221 269

x
false
0
Polygon -7500403 true true 270 75 225 30 30 225 75 270
Polygon -7500403 true true 30 75 75 30 270 225 225 270

@#$#@#$#@
NetLogo 5.2.0
@#$#@#$#@
@#$#@#$#@
@#$#@#$#@
<experiments>
  <experiment name="nosharing" repetitions="1" runMetricsEveryStep="false">
    <setup>setup</setup>
    <go>calculate_yield
stop</go>
    <final>output_stats</final>
    <enumeratedValueSet variable="alpha">
      <value value="0.5"/>
    </enumeratedValueSet>
    <enumeratedValueSet variable="req_adult_eq">
      <value value="5"/>
    </enumeratedValueSet>
  </experiment>
  <experiment name="allocate" repetitions="1" runMetricsEveryStep="false">
    <setup>setup</setup>
    <go>stop</go>
    <enumeratedValueSet variable="alpha">
      <value value="0.5"/>
    </enumeratedValueSet>
    <enumeratedValueSet variable="req_adult_eq">
      <value value="5"/>
    </enumeratedValueSet>
  </experiment>
</experiments>
@#$#@#$#@
@#$#@#$#@
default
0.0
-0.2 0 0.0 1.0
0.0 1 1.0 0.0
0.2 0 0.0 1.0
link direction
true
0
Line -7500403 true 150 150 90 180
Line -7500403 true 150 150 210 180

curved link
1.0
-0.2 0 0.0 1.0
0.0 1 1.0 0.0
0.2 0 0.0 1.0
link direction
true
0
Line -7500403 true 150 150 90 180
Line -7500403 true 150 150 210 180

@#$#@#$#@
0
@#$#@#$#@
